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
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.ApertureDuctEdge
import kotlin.math.abs

class ValveDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeKineticBlockEntity(typeIn, pos, state), IConnectable {

    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.0)
        .chase(0.0, 0.0, LerpedFloat.Chaser.LINEAR)

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

        val axis = ValveDuctBlock.getDuctAxis(blockState)

        val front = blockPos.relative(axis, -1)
        val back = blockPos.relative(axis, 1)
        if (level == null) return
        val backEdge = ClockworkMod.getKelvin(level).getEdgeBetween(getDuctNodePosition(), ClockworkUtils.getDuctNodePos(back, level))
        val frontEdge = ClockworkMod.getKelvin(level).getEdgeBetween(getDuctNodePosition(), ClockworkUtils.getDuctNodePos(front, level))

        (backEdge as? ApertureDuctEdge)?.aperture = pointer.value.toDouble()-backEdge.radius
        (frontEdge as? ApertureDuctEdge)?.aperture = pointer.value.toDouble()-frontEdge.radius
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)

        val target = (if (speed > 0) 1 else 0).toDouble()
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
        return ApertureDuctEdge(ConnectionType.APERTURE, nodeA, nodeB, radius = 0.3125, length = 0.375, aperture = pointer.value.toDouble() - 0.125)
    }

}
