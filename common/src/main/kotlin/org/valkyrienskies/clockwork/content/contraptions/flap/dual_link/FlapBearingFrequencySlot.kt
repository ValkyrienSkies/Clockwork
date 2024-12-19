package org.valkyrienskies.clockwork.content.contraptions.flap.dual_link

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.Direction.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.E

class FlapBearingFrequencySlot(first: Boolean, val front: Boolean) : ValueBoxTransform.Dual(first) {

    override fun getLocalOffset(state: BlockState): Vec3 {
        val facing = state.getValue(BlockStateProperties.FACING)
        val center = Vec3(0.5,0.5,0.5)

        var location = if (front) VecHelper.voxelSpace(16.01, 5.0, 6.0) else VecHelper.voxelSpace(-0.01, 5.0, 6.0)
        if (isFirst) location = location.add(0.0, 6.0/16.0, 0.0)

        if (facing.axis != Axis.Y) location = VecHelper.rotate(location.subtract(center), AngleHelper.horizontalAngle(facing).toDouble(), Axis.Y).add(center)
        else if (facing == UP) location = VecHelper.rotate(location.subtract(center), -90.0, Axis.X).add(center)
        else if (facing == DOWN) location = VecHelper.rotate(location.subtract(center), 90.0, Axis.X).add(center)

        return location
    }

    override fun rotate(state: BlockState, ms: PoseStack) {
        val facing = state.getValue(BlockStateProperties.FACING)
        var xRot: Double
        var yRot: Double

        when (facing) {
            NORTH -> {xRot = 0.0; yRot = 90.0}
            UP, DOWN, SOUTH -> {xRot = 0.0; yRot = -90.0}
            EAST -> {xRot = 0.0; yRot = 0.0}
            else -> {xRot = 0.0; yRot = 180.0}
        }

        if (!front) yRot += 180.0

        TransformStack.cast(ms)
            .rotateX(xRot)
            .rotateY(yRot)


    }

    override fun getScale(): Float {
        return .4975f
    }
}