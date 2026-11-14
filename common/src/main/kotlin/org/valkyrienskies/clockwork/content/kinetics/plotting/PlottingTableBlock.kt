package org.valkyrienskies.clockwork.content.kinetics.plotting

import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.gui.ScreenOpener
import net.createmod.catnip.math.VoxelShaper
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class PlottingTableBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<PlottingTableBlockEntity> {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)) }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(FACING) }
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState = defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    override fun rotate(state: BlockState, rotation: Rotation): BlockState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror): BlockState = state.rotate(mirror.getRotation(state.getValue(FACING)))
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = SHAPE.get(state.getValue(FACING))

    override fun getBlockEntityClass() = PlottingTableBlockEntity::class.java
    override fun getBlockEntityType(): BlockEntityType<out PlottingTableBlockEntity> = ClockworkBlockEntities.PLOTTING_TABLE.get()

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand,
                     hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.PASS
        if (level.isClientSide) withBlockEntityDo(level, pos, ::openScreen)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    @Environment(EnvType.CLIENT)
    private fun openScreen(entity: PlottingTableBlockEntity) = ScreenOpener.open(PlottingTableScreen(entity))

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    companion object {
        private val SHAPE = VoxelShaper.forHorizontal(Shapes.or(
            box(0.0, 12.0, 0.0, 16.0, 14.0, 16.0),
            box(0.25, 0.0, 0.0, 2.25, 2.0, 16.0),
            box(0.25, 10.0, 0.0, 2.25, 12.0, 16.0),
            box(0.25, 2.0, 0.0, 2.25, 10.0, 2.0),
            box(0.25, 2.0, 14.0, 2.25, 10.0, 16.0),
            box(13.75, 0.0, 0.0, 15.75, 2.0, 16.0),
            box(13.75, 10.0, 0.0, 15.75, 12.0, 16.0),
            box(13.75, 2.0, 0.0, 15.75, 10.0, 2.0),
            box(13.75, 2.0, 14.0, 15.75, 10.0, 16.0)
        ), Direction.NORTH)
    }
}
