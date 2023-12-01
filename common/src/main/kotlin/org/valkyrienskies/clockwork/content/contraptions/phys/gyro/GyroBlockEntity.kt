package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState


class GyroBlockEntity(typeIn: BlockEntityType<GyroBlockEntity>, pos: BlockPos, state: BlockState) : KineticBlockEntity(typeIn, pos, state) {

    var visualSpeed = LerpedFloat.linear()
    var angle = 0f

    public override fun write(compound: CompoundTag?, clientPacket: Boolean) {
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag?, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        if (clientPacket) {
            visualSpeed.chase(generatedSpeed.toDouble(), (1 / 64f).toDouble(), Chaser.EXP)
        }
    }

    override fun tick() {
        super.tick()
        if (!level!!.isClientSide) {
            return
        }

        val targetSpeed = getSpeed()
        visualSpeed.updateChaseTarget(targetSpeed)
        visualSpeed.tickChaser()
        angle += visualSpeed.value * 3 / 10f
        angle %= 360f
    }
}