package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.simibubi.create.AllBlocks
import com.simibubi.create.AllShapes
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.STATE
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.config.VSGameConfig
import org.valkyrienskies.mod.common.vsCore

class PhysicalPistonBlock(properties: Properties) : DirectionalAxisKineticBlock(properties), IBE<PhysicalPistonBlockEntity> {
    init {
        registerDefaultState(defaultBlockState().setValue(STATE, PistonState.RETRACTED))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(STATE))
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return when (state.getValue(STATE)) {
            PistonState.MOVING -> OPEN_HOUSING.getValue(facing)
            PistonState.EXTENDED -> AllShapes.MECHANICAL_PISTON_EXTENDED[facing]
            PistonState.RETRACTED -> Shapes.block()
        }
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild() || player.isShiftKeyDown) return InteractionResult.PASS
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty && hand == InteractionHand.MAIN_HAND) {
            if (!level.isClientSide) withBlockEntityDo(level, pos, PhysicalPistonBlockEntity::toggleAssembly)
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        if (state.getValue(STATE) == PistonState.MOVING && hit.direction == state.getValue(FACING).opposite &&
            AllBlocks.PISTON_EXTENSION_POLE.isIn(stack)) {
            if (level.isClientSide) return InteractionResult.SUCCESS
            return onBlockEntityUse(level, pos) { it.addPole(player, hand) }
        }
        return InteractionResult.PASS
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult =
        if (state.getValue(STATE) == PistonState.RETRACTED) super.onWrenched(state, context) else InteractionResult.PASS

    override fun getBlockEntityClass() = PhysicalPistonBlockEntity::class.java

    override fun getBlockEntityType() = ClockworkBlockEntities.PHYS_PISTON.get()

    companion object {
        fun registerPhysicsShape() {
            vsApi.registerBlockStateEvent.on { event ->
                val emptyShape = vsCore.solidShapeUtils.generateShapeFromBoxes(mutableListOf()) ?: return@on
                val solid = event.buildSolidState {
                    shape(emptyShape)
                    friction(VSGameConfig.SERVER.defaultBlockFriction)
                    elasticity(VSGameConfig.SERVER.defaultBlockElasticity)
                    hardness(VSGameConfig.SERVER.defaultBlockHardness)
                }
                ClockworkBlocks.PHYS_PISTON.get().stateDefinition.possibleStates
                    .filter { it.getValue(STATE) == PistonState.MOVING }
                    .forEach { event.register(it, solid) }
            }
        }

        private val OPEN_HOUSING = Direction.entries.associateWith { facing ->
            val bore = when (facing.axis) {
                Direction.Axis.X -> box(0.0, 4.0, 4.0, 16.0, 12.0, 12.0)
                Direction.Axis.Y -> box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0)
                Direction.Axis.Z -> box(4.0, 4.0, 0.0, 12.0, 12.0, 16.0)
            }
            Shapes.join(AllShapes.MECHANICAL_PISTON[facing], bore, BooleanOp.ONLY_FIRST)
        }
    }
}
