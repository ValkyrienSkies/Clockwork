package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

class GimbalBearingFrequencySlot(isFirst: Boolean, val front: Int) : ValueBoxTransform.Dual(isFirst) {

    override fun getLocalOffset(level: LevelAccessor, blockPos: BlockPos, state: BlockState): Vec3 {
        val offsetAlongEdge = if (isFirst) 3.0 else -3.0
        val modelOffset = when (front) {
            1 -> Vec3(8.0 + offsetAlongEdge, 8.0, 1.99)
            2 -> Vec3(14.01, 8.0, 8.0 + offsetAlongEdge)
            3 -> Vec3(8.0 - offsetAlongEdge, 8.0, 14.01)
            else -> Vec3(1.99, 8.0, 8.0 - offsetAlongEdge)
        }

        val rotation = modelRotation(state.getValue(BlockStateProperties.FACING))
        return rotateAroundBlockCenter(modelOffset, rotation.xQuarterTurns, rotation.yQuarterTurns)
            .scale(1.0 / 16.0)
    }

    override fun rotate(level: LevelAccessor, blockPos: BlockPos, state: BlockState, ms: PoseStack) {
        val rotation = modelRotation(state.getValue(BlockStateProperties.FACING))
        TransformStack.of(ms)
            .rotateYDegrees(rotation.yQuarterTurns * 90f)
            .rotateXDegrees(rotation.xQuarterTurns * 90f)
            .rotateYDegrees((1 - front) * 90f)
    }

    override fun getScale(): Float = 0.25f

    companion object {
        fun getSlotDirection(state: BlockState, front: Int): Direction {
            val baseDirection = when (front) {
                1 -> Direction.NORTH
                2 -> Direction.EAST
                3 -> Direction.SOUTH
                else -> Direction.WEST
            }
            val rotation = modelRotation(state.getValue(BlockStateProperties.FACING))
            val directionPoint = Vec3(
                8.0 + baseDirection.stepX,
                8.0 + baseDirection.stepY,
                8.0 + baseDirection.stepZ
            )
            val rotatedDirection = rotateAroundBlockCenter(
                directionPoint,
                rotation.xQuarterTurns,
                rotation.yQuarterTurns
            ).subtract(Vec3(8.0, 8.0, 8.0))
            return Direction.getNearest(rotatedDirection.x, rotatedDirection.y, rotatedDirection.z)
        }

        private fun modelRotation(facing: Direction): ModelRotation = when (facing) {
            Direction.UP -> ModelRotation(0, 0)
            Direction.DOWN -> ModelRotation(2, 0)
            Direction.NORTH -> ModelRotation(1, 0)
            Direction.SOUTH -> ModelRotation(1, 2)
            Direction.WEST -> ModelRotation(1, 3)
            Direction.EAST -> ModelRotation(1, 1)
        }

        private fun rotateAroundBlockCenter(point: Vec3, xQuarterTurns: Int, yQuarterTurns: Int): Vec3 {
            var rotated = point
            repeat(xQuarterTurns) {
                rotated = Vec3(rotated.x, 16.0 - rotated.z, rotated.y)
            }
            repeat(yQuarterTurns) {
                rotated = Vec3(rotated.z, rotated.y, 16.0 - rotated.x)
            }
            return rotated
        }

        private data class ModelRotation(val xQuarterTurns: Int, val yQuarterTurns: Int)
    }
}
