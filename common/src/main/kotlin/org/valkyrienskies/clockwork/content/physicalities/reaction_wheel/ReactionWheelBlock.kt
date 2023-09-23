package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class ReactionWheelBlock(properties: Properties) : RotatedPillarKineticBlock(properties),
    IBE<ReactionWheelBlockEntity> {
    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return AllShapes.LARGE_GEAR[pState.getValue(AXIS)]
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.INVISIBLE
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(AXIS)
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face.axis === getRotationAxis(state)
    }

    override fun getBlockEntityClass(): Class<ReactionWheelBlockEntity> {
        return ReactionWheelBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ReactionWheelBlockEntity> {
        return ClockworkBlockEntities.REACTIONWHEEL.get()
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        getBlockEntity(level, pos)!!.setShouldRemove()
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getParticleTargetRadius(): Float {
        return 2f
    }

    override fun getParticleInitialRadius(): Float {
        return 1.75f
    }
}