package org.valkyrienskies.clockwork.content.contraptions.flap.dual_link

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction.*
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

class FlapBearingFrequencySlot(first: Boolean, val front: Boolean) : ValueBoxTransform.Dual(first) {

    override fun getLocalOffset(level: LevelAccessor, blockPos: BlockPos, state: BlockState): Vec3 {
        val facing = state.getValue(BlockStateProperties.FACING)
        val axis_along = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
        val center = Vec3(0.5,0.5,0.5)

        var location = if (front) VecHelper.voxelSpace(16.01, 5.0, 6.0) else VecHelper.voxelSpace(-0.01, 5.0, 6.0)
        if (isFirst) location = location.add(0.0, 6.0/16.0, 0.0)

        if (facing.axis != Axis.Y) location = VecHelper.rotate(location.subtract(center), AngleHelper.horizontalAngle(facing).toDouble(), Axis.Y).add(center)
        else if (facing == UP) location = VecHelper.rotate(location.subtract(center), -90.0, Axis.X).add(center)
        else if (facing == DOWN) location = VecHelper.rotate(location.subtract(center), 90.0, Axis.X).add(center)

        if (facing.axis == Axis.Y && axis_along) location = VecHelper.rotate(location.subtract(center), -90.0, Axis.Y).add(center)

        return location
    }

    override fun rotate(level: LevelAccessor, blockPos: BlockPos, state: BlockState, ms: PoseStack) {
        val facing = state.getValue(BlockStateProperties.FACING)
        val axis_along = state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
        var xRot: Double
        var yRot: Double

        when (facing) {
            NORTH -> {xRot = 0.0; yRot = 90.0}
            UP, DOWN, SOUTH -> {xRot = 0.0; yRot = -90.0}
            EAST -> {xRot = 0.0; yRot = 0.0}
            else -> {xRot = 0.0; yRot = 180.0}
        }

        if (facing.axis == Axis.Y && axis_along) yRot -= 90.0


        if (!front) yRot += 180.0

        TransformStack.of(ms)
            .rotateXDegrees(xRot.toFloat())
            .rotateYDegrees(yRot.toFloat())


    }

    override fun getScale(): Float {
        return .4975f
    }
}
