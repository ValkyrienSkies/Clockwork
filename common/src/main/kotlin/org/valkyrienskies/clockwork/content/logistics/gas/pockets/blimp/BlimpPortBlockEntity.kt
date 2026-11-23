package org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp

import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.BalloonController.SealedScanResult
import org.valkyrienskies.clockwork.content.forces.BalloonThermodynamics
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.simulatesGas
import org.valkyrienskies.clockwork.util.gui.ClockworkTooltipHelper
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.ValveDuctNode
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.isBlockInShipyard
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class BlimpPortBlockEntity(
    typeIn: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KNodeKineticBlockEntity(typeIn, pos, state), IConnectable, IThinkable {

    enum class ScanStatus {
        NO_POCKET,
        NO_SHIP,
        NOT_SEALED,
        BLOCKED,
        LINKED,
    }

    private val inputSide: Direction
        get() = BlimpPortBlock.getPointerFace(blockState)

    override fun acceptsThinkable(side: Direction) = side == inputSide
    override fun providesThinkable(side: Direction) = side == inputSide.opposite
    override fun thinkableValue(side: Direction): Double? = pointer.value.toDouble().takeIf { providesThinkable(side) }
    override fun thinkablePortName(side: Direction): Component? = when {
        acceptsThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.valve_target")
        providesThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.valve_opening")
        else -> null
    }

    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.0)
        .chase(0.0, 0.0, LerpedFloat.Chaser.LINEAR)

    /** Used by ComputerCraft peripherals to override valve opening (0..1). */
    var computerTarget: Double? = null

    var balloon: BalloonData? = null
        private set
    private var balloonGeometryVersion = -1
    private var connectedBalloonNodePos: DuctNodePos? = null
    private var linkedFacing: Direction? = null

    var scanCooldown = 0
    var shouldScanNextTick = true
    private var telemetrySyncTicks = 0

    // Synced telemetry for goggles, Display Link, and CC
    var hasPocket: Boolean = false
    var scanStatus: ScanStatus = ScanStatus.NO_POCKET
    var balloonVolume: Double = 0.0
    var effectiveVolume: Double = 0.0
    var inflationRatio: Double = 0.0
    var balloonPressure: Double = 0.0
    var balloonTemperature: Double = 0.0
    var balloonLiftMass: Double = 0.0
    var leakCount: Int = 0
    val syncedGases: LinkedHashMap<String, Double> = LinkedHashMap()

    fun requestScan() {
        scanCooldown = 0
        shouldScanNextTick = true
    }

    fun onPortRemoved() {
        val lvl = level ?: return
        if (!lvl.isClientSide) {
            disconnectBalloonEdge(lvl)
        }
        balloon = null
        hasPocket = false
    }

    override fun invalidate() {
        val lvl = level
        if (lvl != null && !lvl.isClientSide) {
            disconnectBalloonEdge(lvl)
        }
        super.invalidate()
    }

    override fun tick() {
        super.tick()
        val lvl = level ?: return
        if (lvl.simulatesGas) updateTarget()
        pointer.tickChaser()

        if (!lvl.simulatesGas || blockState.block !is BlimpPortBlock) return

        val kelvin = ClockworkMod.getKelvin(lvl)
        val valveNode = kelvin.getNodeAt(getDuctNodePosition()) as? ValveDuctNode
        if (valveNode != null) {
            valveNode.radius = pointer.value.toDouble().coerceIn(0.0, 1.0) * BlimpPortBlock.MAX_FLOW_RADIUS
        }

        val serverLevel = lvl as? ServerLevel ?: return
        val facing = blockState.getValue(BlockStateProperties.FACING)

        // If rotated in-place, drop the old link and rescan in the new direction.
        if (linkedFacing != null && linkedFacing != facing) {
            disconnectBalloonEdge(serverLevel)
            balloon = null
            hasPocket = false
            linkedFacing = null
            requestScan()
        }

        val currentBalloon = balloon
        if (currentBalloon != null) {
            val interiorPos = blockPos.relative(facing)
            if (currentBalloon.shouldRemove || !currentBalloon.sealed) {
                disconnectBalloonEdge(serverLevel)
                balloon = null
                hasPocket = false
                scanStatus = ScanStatus.NO_POCKET
                requestScan()
                sendData()
            } else if (balloonGeometryVersion != currentBalloon.geometryVersion || !currentBalloon.containsPosition(interiorPos)) {
                // Re-link in case a split or rescan moved our chamber.
                fetchBalloon(serverLevel)
            } else {
                ensureBalloonEdge(serverLevel, currentBalloon)
                updateTelemetry(serverLevel, currentBalloon)
                if (++telemetrySyncTicks >= 5) {
                    telemetrySyncTicks = 0
                    sendData()
                }
            }
        } else {
            if (scanCooldown > 0) scanCooldown--
            if (shouldScanNextTick || scanCooldown <= 0) {
                shouldScanNextTick = false
                fetchBalloon(serverLevel)
            }
        }
    }

    private fun fetchBalloon(serverLevel: ServerLevel) {
        val facing = blockState.getValue(BlockStateProperties.FACING)
        val ship = serverLevel.getLoadedShipManagingPos(blockPos)
        if (ship == null) {
            disconnectBalloonEdge(serverLevel)
            balloon = null
            hasPocket = false
            setStatus(ScanStatus.NO_SHIP)
            scanCooldown = 40
            return
        }

        val interiorPos = blockPos.relative(facing)
        val controller = BalloonController.getOrCreate(ship)
        val (balloonId, result) = controller.tryGetOrCreateSealedBalloon(interiorPos, serverLevel, shellSeed = blockPos)
        val found = if (result == SealedScanResult.LINKED) controller.getBalloonById(balloonId) else null

        if (found != null && found.sealed) {
            if (balloon !== found) {
                disconnectBalloonEdge(serverLevel)
            }
            balloon = found
            balloonGeometryVersion = found.geometryVersion
            linkedFacing = facing
            hasPocket = true
            scanStatus = ScanStatus.LINKED
            ensureBalloonEdge(serverLevel, found)
            updateTelemetry(serverLevel, found)
            scanCooldown = 40
            telemetrySyncTicks = 0
            sendData()
        } else {
            disconnectBalloonEdge(serverLevel)
            balloon = null
            balloonGeometryVersion = -1
            linkedFacing = null
            hasPocket = false
            syncedGases.clear()
            balloonVolume = 0.0
            effectiveVolume = 0.0
            inflationRatio = 0.0
            balloonPressure = 0.0
            balloonTemperature = 0.0
            balloonLiftMass = 0.0
            leakCount = 0
            setStatus(
                when (result) {
                    SealedScanResult.NOT_SEALED -> ScanStatus.NOT_SEALED
                    SealedScanResult.BLOCKED -> ScanStatus.BLOCKED
                    else -> ScanStatus.NO_POCKET
                }
            )
            scanCooldown = 40
        }
    }

    private fun setStatus(status: ScanStatus) {
        if (scanStatus != status) {
            scanStatus = status
            sendData()
        }
    }

    private fun ensureBalloonEdge(serverLevel: ServerLevel, targetBalloon: BalloonData) {
        val kelvin = ClockworkMod.getKelvin(serverLevel)
        val selfPos = getDuctNodePosition()
        if (kelvin.getNodeAt(selfPos) == null) return

        val balloonNodePos = targetBalloon.ensureKelvinNode(kelvin, serverLevel.dimension().location()) ?: return
        if (connectedBalloonNodePos != null && connectedBalloonNodePos != balloonNodePos) {
            kelvin.removeEdge(selfPos, connectedBalloonNodePos!!)
        }
        if (kelvin.getEdgeBetween(selfPos, balloonNodePos) == null) {
            val edge = PipeDuctEdge(
                type = ConnectionType.PIPE,
                nodeA = selfPos,
                nodeB = balloonNodePos,
                radius = BlimpPortBlock.MAX_FLOW_RADIUS,
                length = 0.375
            ).also { it.wallThermalConductance = 0.0 }
            kelvin.addEdge(selfPos, balloonNodePos, edge)
        }
        connectedBalloonNodePos = balloonNodePos
    }

    private fun disconnectBalloonEdge(level: Level) {
        val targetPos = connectedBalloonNodePos ?: return
        connectedBalloonNodePos = null
        ClockworkMod.getKelvin(level).removeEdge(getDuctNodePosition(), targetPos)
    }

    private fun updateTelemetry(serverLevel: ServerLevel, targetBalloon: BalloonData) {
        val volume = targetBalloon.currentVolume
        balloonVolume = volume
        leakCount = targetBalloon.missingExternalPositions

        val masses = targetBalloon.resolvedMasses()
        val capacity = BalloonThermodynamics.capacity(masses)
        balloonTemperature = if (capacity > 1e-9) targetBalloon.currentEnergy / capacity else 0.0
        balloonPressure = BalloonThermodynamics.pressure(masses, targetBalloon.currentEnergy, volume)

        val ship = serverLevel.getLoadedShipManagingPos(blockPos)
        if (ship != null && volume > 1e-9) {
            val center = targetBalloon.getCenter()
            val yWorld = ship.transform.positionToWorld(Vector3d(center)).y
            val atmosphere = serverLevel.shipObjectWorld.aerodynamicUtils
            val ambientPressure = atmosphere.getAirPressureForY(yWorld, serverLevel.dimensionId)
            val ambientDensity = atmosphere.getAirDensityForY(yWorld, serverLevel.dimensionId)
            effectiveVolume = BalloonThermodynamics.effectiveVolume(volume, balloonPressure, ambientPressure)
            inflationRatio = (effectiveVolume / volume).coerceIn(0.0, 1.0)
            balloonLiftMass = max(0.0, ambientDensity * effectiveVolume - masses.values.sum())
        } else {
            effectiveVolume = 0.0
            inflationRatio = 0.0
            balloonLiftMass = 0.0
        }

        syncedGases.clear()
        for ((key, mass) in targetBalloon.gasMasses) {
            if (mass.isFinite() && mass > 1e-6) {
                syncedGases[key] = mass
            }
        }
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        updateTarget()
        sendData()
    }

    fun updateTarget() {
        var target = (if (speed > 0) 1 else 0).toDouble()

        computerTarget?.let { target = it.coerceIn(0.0, 1.0) }
        ThinkableConnections.input(level, blockPos, inputSide)?.let { target = (it.value ?: 0.0).coerceIn(0.0, 1.0) }

        val changed = pointer.chaseTarget != target.toFloat()
        pointer.chase(target, getChaseSpeed(), LerpedFloat.Chaser.LINEAR)

        if (changed) sendData()
    }

    private fun getChaseSpeed(): Double = Mth.clamp(abs(getSpeed().toDouble()) / 16.0 / 40.0, 0.0, 1.0)

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.put("Pointer", pointer.writeNBT())
        compound.putBoolean("HasPocket", hasPocket)
        compound.putInt("ScanStatus", scanStatus.ordinal)
        compound.putDouble("BalloonVolume", balloonVolume)
        compound.putDouble("EffectiveVolume", effectiveVolume)
        compound.putDouble("InflationRatio", inflationRatio)
        compound.putDouble("BalloonPressure", balloonPressure)
        compound.putDouble("BalloonTemperature", balloonTemperature)
        compound.putDouble("BalloonLiftMass", balloonLiftMass)
        compound.putInt("LeakCount", leakCount)
        val gasesTag = CompoundTag()
        for ((key, mass) in syncedGases) {
            gasesTag.putDouble(key, mass)
        }
        compound.put("SyncedGases", gasesTag)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        pointer.readNBT(compound.getCompound("Pointer"), clientPacket)
        hasPocket = compound.getBoolean("HasPocket")
        val statusOrdinal = compound.getInt("ScanStatus")
        scanStatus = ScanStatus.values().getOrElse(statusOrdinal) { ScanStatus.NO_POCKET }
        balloonVolume = compound.getDouble("BalloonVolume")
        effectiveVolume = compound.getDouble("EffectiveVolume")
        inflationRatio = compound.getDouble("InflationRatio")
        balloonPressure = compound.getDouble("BalloonPressure")
        balloonTemperature = compound.getDouble("BalloonTemperature")
        balloonLiftMass = compound.getDouble("BalloonLiftMass")
        leakCount = compound.getInt("LeakCount")
        syncedGases.clear()
        if (compound.contains("SyncedGases")) {
            val gasesTag = compound.getCompound("SyncedGases")
            for (key in gasesTag.allKeys) {
                syncedGases[key] = gasesTag.getDouble(key)
            }
        }
    }

    override fun getEdge(
        nodeA: DuctNodePos,
        nodeB: DuctNodePos,
        level: Level,
        blockPos: BlockPos,
        direction: Direction
    ): DuctEdge = PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = BlimpPortBlock.MAX_FLOW_RADIUS, length = 0.375)

    override fun setEdge(
        nodeA: DuctNodePos,
        nodeB: DuctNodePos,
        level: Level,
        blockPos: BlockPos,
        direction: Direction
    ) {
        if (ClockworkMod.getKelvin(level).getEdgeBetween(nodeA, nodeB) != null) return
        super<IConnectable>.setEdge(nodeA, nodeB, level, blockPos, direction)
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        val list = tooltip as MutableList<Component>
        ClockworkLang.translate("gui.blimp_port.info.title").forGoggles(list)

        val valvePercent = (pointer.value * 100f).roundToInt().coerceIn(0, 100)
        ClockworkLang.translate("gui.blimp_port.info.valve", "$valvePercent%")
            .style(if (valvePercent > 0) ChatFormatting.AQUA else ChatFormatting.GRAY)
            .forGoggles(list)

        if (!level.isBlockInShipyard(blockPos.x, blockPos.y, blockPos.z)) {
            ClockworkTooltipHelper.addHint(list, "gui.gas_nozzle.info.no_ship", ChatFormatting.GOLD)
        } else if (!hasPocket) {
            when (scanStatus) {
                ScanStatus.NOT_SEALED -> ClockworkTooltipHelper.addHint(
                    list, "gui.blimp_port.info.not_sealed", ChatFormatting.RED, 0, 0,
                    Minecraft.getInstance().options.keyUse.translatedKeyMessage
                )
                ScanStatus.BLOCKED -> ClockworkTooltipHelper.addHint(
                    list, "gui.blimp_port.info.blocked", ChatFormatting.GOLD
                )
                else -> ClockworkTooltipHelper.addHint(
                    list, "gui.blimp_port.info.no_pocket", ChatFormatting.GOLD, 0, 0,
                    Minecraft.getInstance().options.keyUse.translatedKeyMessage
                )
            }
        } else {
            ClockworkLang.translate(
                "gui.blimp_port.info.volume",
                DuctTextUtil.translateVolume(ClockworkLang.builder(), balloonVolume, true)
            ).style(ChatFormatting.GREEN).forGoggles(list)

            val inflationPercent = (inflationRatio * 100.0).roundToInt().coerceIn(0, 100)
            ClockworkLang.translate("gui.blimp_port.info.inflation", "$inflationPercent%")
                .style(if (inflationPercent > 0) ChatFormatting.AQUA else ChatFormatting.GRAY)
                .forGoggles(list)

            ClockworkLang.translate(
                "gui.blimp_port.info.pressure",
                DuctTextUtil.translatePressure(ClockworkLang.builder(), balloonPressure, true)
            ).style(ChatFormatting.YELLOW).forGoggles(list)

            if (balloonTemperature > 1e-3) {
                ClockworkLang.translate(
                    "gui.blimp_port.info.temperature",
                    DuctTextUtil.translateTemperature(ClockworkLang.builder(), balloonTemperature, true)
                ).style(ChatFormatting.GOLD).forGoggles(list)
            }

            if (syncedGases.isEmpty()) {
                ClockworkLang.translate("gui.blimp_port.info.empty")
                    .style(ChatFormatting.DARK_GRAY)
                    .forGoggles(list, 1)
            } else {
                for ((key, mass) in syncedGases.entries.sortedByDescending { it.value }) {
                    val loc = ResourceLocation.tryParse(key) ?: continue
                    val gasType = GasTypeRegistry.getGasType(loc) ?: continue
                    ClockworkLang.builder()
                        .add(DuctTextUtil.gasComponent(gasType, mass, isPlayerSneaking))
                        .forGoggles(list, 1)
                }
            }

            if (leakCount > 0) {
                list.add(CommonComponents.EMPTY)
                ClockworkTooltipHelper.addHint(list, "gui.gas_nozzle.info.leak", ChatFormatting.RED, -2)
                ClockworkLang.translate("gui.gas_nozzle.info.leak.count", leakCount)
                    .style(ChatFormatting.RED)
                    .forGoggles(list)
            }
        }

        list.add(CommonComponents.EMPTY)
        return super.addToGoggleTooltip(list, isPlayerSneaking)
    }
}
