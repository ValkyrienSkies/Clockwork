package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceTelemetry
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceSource
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceAccumulator
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.clockwork.util.sound.GasThrusterSound
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.clockwork.util.kelvin.KelvinParticleHelper
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*

@OptIn(VsBeta::class)
class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), BlockEntityPhysicsListener, AeronautForceSource, IThinkable {

    companion object {
        internal const val NOZZLE_PIVOT = 6.0 / 16.0
        internal const val NOZZLE_LENGTH = 1.0 - NOZZLE_PIVOT
        private const val MAX_GIMBAL_PITCH = 30.0
        private const val MIN_VISIBLE_GAS_KG_PER_TICK = 0.000001
        private val AIR_ID = ResourceLocation("kelvin", "air")
    }

    @Volatile
    override lateinit var dimension: DimensionId

    @Volatile
    var thrust = 0.0
    @Volatile
    private var gimbalPitch = 0.0
    private val facing get() = blockState.getValue(BlockStateProperties.FACING)
    private val gimbalSide get() = if (facing.axis.isHorizontal) Direction.UP else Direction.NORTH

    var velocity = 0.0
    private var chamberHeat = 0.0
    internal val normalizedTemperature get() = ((chamberHeat - 550.0) / 900.0).coerceIn(0.0, 1.0)
    internal val stellarSteam get() = ((chamberHeat - 1025.0) / 75.0).coerceIn(0.0, 1.0).toFloat() *
        (steamFraction - exhaustFraction).coerceAtLeast(0f)
    internal val violetExhaust get() = ((chamberHeat - 1480.0) / 140.0).coerceIn(0.0, 1.0).toFloat() *
        (exhaustFraction - 8f * steamFraction).coerceAtLeast(0f)
    private var nonAirFlow = 0.0
    var expansion = 0f
        private set
    var plumeHeat = 0f
        private set
    var steamFraction = 0f
        private set
    var vaporStrength = 0f
        private set
    var exhaustFraction = 0f
        private set
    var previousHeatSpread = 0f
        private set
    var heatSpread = 0f
        private set
    var previousLipGlow = 0f
        private set
    var lipGlow = 0f
        private set
    val plumeLength get() = (0.45f + 2.8f * (velocity / 800.0).coerceIn(0.0, 1.4).toFloat()) *
        (0.45f + 0.85f * max(plumeHeat, vaporStrength)) * (1f - 0.35f * steamFraction)

    val gasMassFlow = HashMap<GasType, Double>()
    val massPerParticle = 0.005

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        val compound = CompoundTag()
        for ((gas, mass) in gasMassFlow) {
            compound.putDouble(gas.resourceLocation.toString(), mass)
        }
        tag.put("GasMassFlow", compound)
        tag.putDouble("FlowRate",velocity)
        tag.putDouble("Thrust",thrust)
        tag.putDouble("PlumeTemperature", chamberHeat)
        tag.putFloat("PlumeExpansion", expansion)
        if (clientPacket) tag.putDouble("GimbalPitch", gimbalPitch)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {

        super.read(tag, clientPacket)
        gasMassFlow.clear()
        val compound = tag.getCompound("GasMassFlow")
        for (key in compound.allKeys) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
            gasMassFlow[gasType] = compound.getDouble(key)
        }

        velocity = tag.getDouble("FlowRate")
        thrust = tag.getDouble("Thrust")
        chamberHeat = tag.getDouble("PlumeTemperature")
        expansion = tag.getFloat("PlumeExpansion")
        gimbalPitch = if (clientPacket) tag.getDouble("GimbalPitch") else 0.0
        nonAirFlow = gasMassFlow.entries.sumOf { (gas, mass) -> if (gas.resourceLocation == AIR_ID) 0.0 else mass }
        val total = nonAirFlow.takeIf { it >= MIN_VISIBLE_GAS_KG_PER_TICK } ?: 0.0
        steamFraction = if (total > 0) ((gasMassFlow[ClockworkGasses.STEAM] ?: 0.0) / total).toFloat() else 0f
        exhaustFraction = if (total > 0) ((gasMassFlow[ClockworkGasses.EXHAUST] ?: 0.0) / total).toFloat() else 0f
        vaporStrength = ((chamberHeat - 350.0) / 450.0).coerceIn(0.0, 1.0).toFloat() * steamFraction
        plumeHeat = (normalizedTemperature *
            sqrt((4.0 * exhaustFraction).coerceIn(0.0, 1.0))).toFloat()

    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun acceptsThinkable(side: Direction) = side == gimbalSide

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gimbal_pitch") else null

    private fun requestedGimbalPitch(level: ServerLevel): Double {
        val wheelThrottle = DrivingControlLinks.get(level).throttle(level, blockPos)
        val source = ThinkableConnections.sourceAt(level, blockPos, gimbalSide, cableOnly = wheelThrottle != null)
        if (source == null) return -(wheelThrottle ?: 0f) * MAX_GIMBAL_PITCH
        return source.device?.thinkableValue(source.side, "")?.takeIf { it.isFinite() }
            ?.coerceIn(-MAX_GIMBAL_PITCH, MAX_GIMBAL_PITCH) ?: 0.0
    }

    private fun thrustDirection() = facing.normal.toJOMLD().let { direction ->
        val angle = Math.toRadians(gimbalPitch)
        direction.mul(cos(angle)).fma(-sin(angle), gimbalSide.normal.toJOMLD())
    }

    internal fun exhaustDirection() = thrustDirection().negate()

    internal data class PlumeImpact(val distance: Double, val normal: Vector3d)

    internal var plumeImpact: PlumeImpact? = null
        private set

    private fun tracePlume(world: ClientLevel, mouth: Vector3d, direction: Vector3d): PlumeImpact? {
        val source = world.getLoadedShipManagingPos(blockPos)?.renderTransform
        val start = Vector3d(mouth).fma(-0.40, direction)
        val end = Vector3d(mouth).fma(plumeLength * 1.16 + 0.18, direction)
        source?.shipToWorld?.transformPosition(start)
        source?.shipToWorld?.transformPosition(end)
        val hit = world.clipIncludeShips(ClipContext(start.toMinecraft(), end.toMinecraft(),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null))
        if (hit.type == HitResult.Type.MISS) return null
        val point = Vector3d(hit.location.x, hit.location.y, hit.location.z)
        val normal = hit.direction.normal.toJOMLD()
        world.getLoadedShipManagingPos(hit.blockPos)?.renderTransform?.shipToWorld?.transformDirection(normal)
        source?.worldToShip?.transformPosition(point)
        source?.worldToShip?.transformDirection(normal)
        normal.normalize()
        if (normal.dot(direction) >= -0.0001) return null
        return PlumeImpact(point.sub(mouth).dot(direction), normal)
    }

    fun clientTick() {
        val level = level ?: return
        previousLipGlow = lipGlow
        val showAir = nonAirFlow < MIN_VISIBLE_GAS_KG_PER_TICK
        val flow = if (showAir) gasMassFlow.values.sum() else nonAirFlow
        val metalTarget = (normalizedTemperature *
            (1.0 - exp(-flow / 0.0015))).toFloat()
        lipGlow += (metalTarget - lipGlow) * if (metalTarget > lipGlow) 0.11f else 0.018f
        previousHeatSpread = heatSpread
        heatSpread += (lipGlow - heatSpread) * if (lipGlow > heatSpread) 0.026f else 0.045f
        plumeImpact = null
        if (gasMassFlow.isEmpty() || velocity == 0.0) return

        if (level is ClientLevel) GasThrusterSound.start(this)

        // Handle particles
        val outward = facing.opposite
        val exhaustDirection = exhaustDirection()
        val speed = (abs(velocity) / 950.0).coerceAtMost(1.15)
        val mouth = blockPos.toJOMLD().add(0.5, 0.5, 0.5).fma(NOZZLE_PIVOT, outward.normal.toJOMLD())
            .fma(NOZZLE_LENGTH, exhaustDirection)
        val ductNodePos = getDuctNodePosition()

        val impact = (level as? ClientLevel)?.let { tracePlume(it, mouth, exhaustDirection) }
        plumeImpact = impact
        val contact = impact?.let { Vector3d(mouth).fma(it.distance, exhaustDirection) }
        val jetStrength = max(plumeHeat, vaporStrength)
        for ((gas, mass) in gasMassFlow) {
            if (!showAir && gas.resourceLocation == AIR_ID) continue
            val particleCount = mass / massPerParticle
            KelvinParticleHelper.spawnJetForGas(
                level,
                ductNodePos,
                gas,
                mouth,
                outward,
                speed,
                particleCount * (1.0 - 0.65 * plumeHeat * exhaustFraction),
                radius = 0.24 - 0.16 * jetStrength,
                outwardOffset = min(1f, jetStrength * 8f) * plumeLength * 0.55,
                jetDirection = exhaustDirection
            ) { pos, vel ->
                if (impact != null && contact != null) {
                    val offset = Vector3d(pos).sub(contact)
                    val gap = offset.dot(impact.normal)
                    if (gap < 0.04) {
                        pos.fma(0.04 - gap, impact.normal)
                        offset.fma(-gap, impact.normal)
                        vel.fma(-vel.dot(impact.normal), impact.normal)
                        if (offset.lengthSquared() > 0.0001) vel.fma(speed * 0.55, offset.normalize())
                    }
                }
            }
        }
    }

    private fun clearMassFlow() {
        gasMassFlow.clear()
        velocity = 0.0
        chamberHeat = 0.0
        expansion = 0f
        thrust = 0.0
    }

    @OptIn(GameTickOnly::class)
    override fun tick() {
        super.tick()
        val currentLevel = level ?: return clearMassFlow()
        if (currentLevel.isClientSide) return clientTick()
        if (currentLevel !is ServerLevel) return

        val nextPitch = requestedGimbalPitch(currentLevel)
        val pitchChanged = gimbalPitch != nextPitch
        gimbalPitch = nextPitch
        val hadExhaust = gasMassFlow.isNotEmpty() || velocity != 0.0 || thrust != 0.0
        tickExhaust(currentLevel)
        if (pitchChanged || hadExhaust || gasMassFlow.isNotEmpty()) sendData()
    }

    @OptIn(GameTickOnly::class)
    private fun tickExhaust(currentLevel: ServerLevel) {
        val ductNodePos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin(currentLevel)
        val node = kelvin.getNodeAt(ductNodePos) as? GasThrusterDuctNode ?: return clearMassFlow()

        val worldPosition = currentLevel.toWorldCoordinates(blockPos.toJOMLD().add(0.5, 0.5, 0.5))
        val exhaustNeighbor = blockPos.relative(facing.opposite)
        val worldExhaustCenter = currentLevel.toWorldCoordinates(Vec3.atCenterOf(exhaustNeighbor))
        if (!worldPosition.isFinite || !worldExhaustCenter.x.isFinite() ||
            !worldExhaustCenter.y.isFinite() || !worldExhaustCenter.z.isFinite()) {
            node.environmentValid = false
            return clearMassFlow()
        }
        val aero = (currentLevel.shipObjectWorld as? VsiServerShipWorld)?.aerodynamicUtils
        val ambientPressure = aero?.getAirPressureForY(worldPosition.y, currentLevel.dimensionId)
        val ambientTemperature = aero?.getAirTemperatureForY(worldPosition.y, currentLevel.dimensionId)

        if (ambientPressure == null || !ambientPressure.isFinite() || ambientPressure < 0.0) {
            node.environmentValid = false
            return clearMassFlow()
        }

        val worldExhaustPos = BlockPos.containing(worldExhaustCenter)
        val localState = if (currentLevel.hasChunkAt(exhaustNeighbor)) currentLevel.getBlockState(exhaustNeighbor) else null
        val worldState = if (currentLevel.hasChunkAt(worldExhaustPos)) currentLevel.getBlockState(worldExhaustPos) else null
        val nozzleOpenToAir = localState != null && worldState != null &&
            localState.fluidState.isEmpty && localState.getCollisionShape(currentLevel, exhaustNeighbor).isEmpty &&
            worldState.fluidState.isEmpty && worldState.getCollisionShape(currentLevel, worldExhaustPos).isEmpty

        node.ambientPressure = ambientPressure
        node.ambientTemperature = ambientTemperature?.takeIf { it.isFinite() && it > 0.0 } ?: 293.15
        node.ambientAirGas = GasTypeRegistry.getGasType(AIR_ID)
        node.nozzleOpenToAir = nozzleOpenToAir
        node.environmentValid = true
        if (nozzleOpenToAir && ambientPressure > 0.0) {
            GasThrusterDuctNode.diffuseOpenTailpipe(kelvin, ductNodePos, ambientPressure, 0.05)
        }

        val summary = node.drainTickSummary()
        if (summary.totalExpelledMass <= 0.0 || summary.averageRawThrust <= 0.0) return clearMassFlow()

        gasMassFlow.clear()
        gasMassFlow.putAll(summary.expelledGasMasses)
        velocity = summary.averageVelocity
        chamberHeat = summary.averageTemperature
        expansion = summary.averageExpansion
        thrust = (summary.averageRawThrust * ClockworkConfig.SERVER.gasThruster.gasThrusterForceMul)
            .takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    }

    @OptIn(PhysTickOnly::class)
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        physShip?: return
        if (blockState.block !is GasThrusterBlock) return
        val force = thrustDirection().mul(thrust)
        physShip.applyModelForce(force, blockPos.toJOMLD().add(0.5,0.5,0.5))
        val source = "gas:${blockPos.asLong()}"
        val readout = AeronautForceTelemetry.begin(physShip.id, source)
        readout?.add(blockPos.toJOMLD().add(0.5, 0.5, 0.5), force)
        AeronautForceTelemetry.publish(physShip.id, source, readout)
    }

    override fun addPotentialForces(level: ServerLevel, ship: LoadedServerShip, thrust: AeronautForceAccumulator) {
        // Gas jets have no nameplate thrust: available output depends on their gas supply.
        thrust.add(blockPos.toJOMLD().add(0.5, 0.5, 0.5),
            thrustDirection().mul(this.thrust))
    }

}
