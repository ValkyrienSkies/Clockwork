package org.valkyrienskies.clockwork.content.logistics.gas.valve

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.abs

class ValveDuctBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : KineticBlockEntity(typeIn, pos,state) {

    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.0)
        .chase(0.0, 0.0, LerpedFloat.Chaser.LINEAR)

    override fun tick() {
        super.tick()
        pointer.tickChaser()

        if (level == null || level!!.isClientSide) return
        val state = level!!.getBlockState(blockPos) ?: return
        val block = state.block as ValveDuctBlock

        if (block.edge == null) return

        block.edge!!.aperture = pointer.value.toDouble()-block.edge!!.radius
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
}