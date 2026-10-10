package org.valkyrienskies.clockwork.content.logistics.gas.valve

import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.ValveDuctNode
import kotlin.math.abs

class ValveDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeKineticBlockEntity(typeIn, pos, state), IConnectable {

    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.0)
        .chase(0.0, 0.0, LerpedFloat.Chaser.LINEAR)

    /**
     * Used for Computer Craft computers to set the target angle
     */
    var computerTarget: Double? = null

    override fun lazyTick() {
        super.lazyTick()

        if (level?.isClientSide != false) return
        if (blockState.block !is ValveDuctBlock) return

        val dir = Direction.get(Direction.AxisDirection.POSITIVE, ValveDuctBlock.getDuctAxis(blockState))
        updateConnection(level!!, blockPos, dir)
        updateConnection(level!!, blockPos, dir.opposite)
    }


    override fun tick() {
        super.tick()
        pointer.tickChaser()

        if (level == null || level!!.isClientSide || blockState.block !is ValveDuctBlock) return

        val valveNode = ClockworkMod.getKelvin(level).getNodeAt(getDuctNodePosition()) as? ValveDuctNode ?: return
        valveNode.radius = pointer.value.toDouble().coerceIn(0.0, 1.0) * ValveDuctBlock.MAX_FLOW_RADIUS
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)

        updateTarget()
    }

    fun updateTarget() {
        var target = (if (speed > 0) 1 else 0).toDouble()

        computerTarget?.let {
            target = it
        }

        pointer.chase(target, getChaseSpeed(), LerpedFloat.Chaser.LINEAR)

        sendData()
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
