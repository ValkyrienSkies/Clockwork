package org.valkyrienskies.clockwork.content.contraptions.flap

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

class FlapBearingFrequencySlot(first: Boolean, val front: Boolean) : ValueBoxTransform.Dual(first) {

    override fun getLocalOffset(state: BlockState): Vec3 {
        val facing = state.getValue(BlockStateProperties.FACING)
        var location = if (front) VecHelper.voxelSpace(-0.01, 6.0, 5.0) else VecHelper.voxelSpace(16.01, 6.0, 5.0)

        if (isFirst) location = location.add(0.0, 0.0, (6 / 16f).toDouble())
        if (facing.axis != Axis.Y) location = location.add(VecHelper.rotate(VecHelper.voxelSpace(0.0, 0.0, -5.0), AngleHelper.horizontalAngle(facing).toDouble(), Axis.Y))



        return location
    }

    override fun rotate(state: BlockState, ms: PoseStack) {
        val facing = state.getValue(BlockStateProperties.FACING)
        val xRot: Float
        val yRot: Float
        if (facing.axis.isVertical) {
            yRot = 270f
            xRot = AngleHelper.verticalAngle(facing)
        } else {
            yRot = AngleHelper.horizontalAngle(facing) + 270
            xRot = 0f
        }
        TransformStack.cast(ms)
            .rotateX(xRot.toDouble())
            .rotateY(yRot.toDouble())

        if (!front) {
            TransformStack.cast(ms).rotateY(-180.0)
        }
    }

    override fun getScale(): Float {
        return .4975f
    }
}