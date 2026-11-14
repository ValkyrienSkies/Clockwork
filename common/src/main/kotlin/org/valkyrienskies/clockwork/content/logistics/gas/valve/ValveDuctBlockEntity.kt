package org.valkyrienskies.clockwork.content.logistics.gas.valve

import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.ValveDuctNode
import kotlin.math.abs

class ValveDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeKineticBlockEntity(typeIn, pos, state), IConnectable, IThinkable {
    private val inputSide get() = Direction.entries.first {
        it.axis != blockState.getValue(BlockStateProperties.FACING).axis && it.axis != ValveDuctBlock.getDuctAxis(blockState) &&
            it.axisDirection == Direction.AxisDirection.POSITIVE
    }

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

    /**
     * Used for Computer Craft computers to set the target angle
     */
    var computerTarget: Double? = null

    override fun tick() {
        super.tick()
        if (level?.isClientSide == false) updateTarget()
        pointer.tickChaser()

        if (level == null || level!!.isClientSide || blockState.block !is ValveDuctBlock) return

        val valveNode = ClockworkMod.getKelvin(level).getNodeAt(getDuctNodePosition()) as? ValveDuctNode ?: return
        valveNode.radius = pointer.value.toDouble().coerceIn(0.0, 1.0) * ValveDuctBlock.MAX_FLOW_RADIUS
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)

        updateTarget()
        sendData()
    }

    fun updateTarget() {
        var target = (if (speed > 0) 1 else 0).toDouble()

        computerTarget?.let {
            target = it
        }
        ThinkableConnections.input(level, blockPos, inputSide)?.let { target = (it.value ?: 0.0).coerceIn(0.0, 1.0) }

        val changed = pointer.chaseTarget != target.toFloat()
        pointer.chase(target, getChaseSpeed(), LerpedFloat.Chaser.LINEAR)

        if (changed) sendData()
    }

    private fun getChaseSpeed(): Double {
        return Mth.clamp(abs(getSpeed().toDouble()) / 16.0 / 40.0, 0.0, 1.0)
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.put("Pointer", pointer.writeNBT())
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        pointer.readNBT(compound.getCompound("Pointer"), clientPacket)
    }

    override fun getEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction): DuctEdge {
        return PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375)
    }

    override fun setEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction) {
        if (ClockworkMod.getKelvin(level).getEdgeBetween(nodeA, nodeB) != null) return
        super<IConnectable>.setEdge(nodeA, nodeB, level, blockPos, direction)
    }

}
