package org.valkyrienskies.clockwork.content.logistics.gas.combustor

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.configureMetalDuctEdge
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD

class GasCombustorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), IConnectable, IThinkable {

    private var sparkCooldown = 0
    private var lastSyncedFacing: Direction? = null

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun acceptsThinkable(side: Direction) = side == Direction.UP

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gas_throttle") else null

    private fun throttle(world: ServerLevel): Double {
        val linkedThrottle = DrivingControlLinks.get(world).linkedThrottle(world, blockPos)
        val source = ThinkableConnections.sourceAt(world, blockPos, Direction.UP, cableOnly = linkedThrottle != null)
            ?: return linkedThrottle ?: (world.getBestNeighborSignal(blockPos) / 15.0)
        val device = source.device ?: return 0.0
        val reading = if (device.thinkableReadings(source.side).any { it.id() == "throttle" }) "throttle" else ""
        return device.thinkableValue(source.side, reading)?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
    }

    override fun setEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction) {
        val kelvin = ClockworkMod.getKelvin(level)
        if (kelvin.getEdgeBetween(nodeA, nodeB) != null) return
        val edge = getEdge(nodeA, nodeB, level, blockPos, direction)
        val exhaustFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        if (direction == exhaustFacing) {
            configureMetalDuctEdge(edge)
        } else {
            edge.wallThermalConductance = 0.0
            edge.thermalConductivityMultiplier = 0.0
        }
        kelvin.addEdge(nodeA, nodeB, edge)
    }

    internal fun syncNodeState(throttleValue: Double? = null) {
        val world = level as? ServerLevel ?: return
        val network = ClockworkMod.getKelvin(world)
        val node = network.getNodeAt(getDuctNodePosition()) as? CombustorDuctNode ?: return
        val facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        val facingChanged = node.facing != facing || lastSyncedFacing != facing
        node.facing = facing
        lastSyncedFacing = facing
        if (throttleValue != null) {
            node.throttle = throttleValue
        }
        node.insulateInletEdges()
        if (facingChanged) {
            val exhaustPos = blockPos.relative(facing)
            val exhaustNodePos = DuctNodePos(
                exhaustPos.x.toDouble(), exhaustPos.y.toDouble(), exhaustPos.z.toDouble(),
                world.dimension().location(),
            )
            network.getEdgeBetween(getDuctNodePosition(), exhaustNodePos)?.let { configureMetalDuctEdge(it) }
        }
    }

    fun triggerManualSpark(world: ServerLevel): Boolean {
        if (dataToLoad != null) return false
        if (sparkCooldown > AUTO_SPARK_COOLDOWN_TICKS - MANUAL_SPARK_MIN_INTERVAL_TICKS) return false
        return fireSpark(world)
    }

    private fun fireSpark(world: ServerLevel): Boolean {
        val network = ClockworkMod.getKelvin(world)
        val position = getDuctNodePosition()
        if (network.getNodeAt(position) == null) return false

        val result = CombustionSpark.ignite(world, network, position)
        sparkCooldown = AUTO_SPARK_COOLDOWN_TICKS

        val center = world.toWorldCoordinates(blockPos.toJOMLD().add(0.5, 0.5, 0.5))
        world.playSound(
            null,
            center.x, center.y, center.z,
            if (result.ignited) SoundEvents.FIRECHARGE_USE else SoundEvents.FLINTANDSTEEL_USE,
            SoundSource.BLOCKS,
            if (result.ignited) 0.45f else 0.35f,
            if (result.ignited) 1.2f else 1.65f,
        )
        world.sendParticles(
            if (result.ignited) ParticleTypes.FLAME else ParticleTypes.ELECTRIC_SPARK,
            center.x, center.y + 0.55, center.z,
            if (result.ignited) 6 else 4,
            0.15, 0.1, 0.15,
            0.02,
        )
        return result.ignited
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null) return

        val currentThrottle = throttle(world)
        syncNodeState(currentThrottle)

        if (sparkCooldown > 0) sparkCooldown--
        if (currentThrottle <= 0.0 || sparkCooldown > 0) return

        val network = ClockworkMod.getKelvin(world)
        val position = getDuctNodePosition()
        val node = network.getNodeAt(position) as? CombustorDuctNode ?: return
        val temp = network.getTemperatureAt(position)
        if (!temp.isFinite() || temp >= AUTO_IGNITE_THRESHOLD_K) return

        val chamberMasses = network.getGasMassAt(position)
        val chamberMass = chamberMasses.values.sum()
        if (chamberMass <= MIN_SPARK_GAS_MASS_KG) return

        val hasInletGas = node.nodeEdges.any { edge ->
            if (!node.isInletEdge(edge)) return@any false
            val other = if (edge.nodeA == position) edge.nodeB else edge.nodeA
            network.getGasMassAt(other).values.sum() > MIN_SPARK_GAS_MASS_KG
        }
        val hasNonAirInChamber = chamberMasses.any { (gas, mass) ->
            mass > MIN_SPARK_GAS_MASS_KG && gas.resourceLocation != AIR_ID
        }
        if (!hasInletGas && !hasNonAirInChamber) return

        fireSpark(world)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (!clientPacket) tag.putInt("SparkCooldown", sparkCooldown)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (!clientPacket) sparkCooldown = tag.getInt("SparkCooldown").coerceIn(0, AUTO_SPARK_COOLDOWN_TICKS)
    }

    companion object {
        const val AUTO_SPARK_COOLDOWN_TICKS = 40
        private const val MANUAL_SPARK_MIN_INTERVAL_TICKS = 10
        private const val AUTO_IGNITE_THRESHOLD_K = 750.0
        private const val MIN_SPARK_GAS_MASS_KG = 1e-4
        private val AIR_ID = ResourceLocation("kelvin", "air")
    }
}
