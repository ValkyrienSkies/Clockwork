package org.valkyrienskies.clockwork.content.contraptions.phys.infuser


import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class PhysicsInfuserBlock(properties: Properties) : Block(properties),
    IBE<PhysicsInfuserBlockEntity> {
    override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, moved: Boolean) {
        if (oldState.block === state.block) {
            return
        }
        if (moved) {
        }
    }

    override fun use(
        state: BlockState,
        worldIn: Level,
        pos: BlockPos,
        player: Player,
        handIn: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (worldIn.isClientSide) {
            return InteractionResult.PASS
        }
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(handIn).isEmpty) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(
                    worldIn, pos
                ) { te: PhysicsInfuserBlockEntity? -> if (te!!.isAssembled && !te.assembling && !te.disassembling) te.startDisassembly() }
                withBlockEntityDo(
                    worldIn, pos
                ) { te: PhysicsInfuserBlockEntity? -> if (!te!!.isAssembled && !te.assembling && !te.disassembling) te.startAssembly() }
                return InteractionResult.SUCCESS
            }
            withBlockEntityDo(
                worldIn, pos
            ) { te: PhysicsInfuserBlockEntity? ->
                if (te!!.isAssembled && !te.assembling && !te.disassembling && !te.onCooldown) {
                    te.startDisassembly()
                } else if (!te.isAssembled && te.assembling && !te.disassembling && !te.onCooldown) {
                    te.skipAssembly()
                } else if (!te.isAssembled && !te.assembling && !te.disassembling && !te.onCooldown) {
                    te.startAssembly()
                }
            }
            return InteractionResult.SUCCESS
        } else if (player.getItemInHand(handIn).item is Nothing) { //todo replace with wanderwand when wanderwand rework done
            if (worldIn.getBlockEntity(pos) != null) {
                if (worldIn.getBlockEntity(pos) is PhysicsInfuserBlockEntity) {
                    val te: PhysicsInfuserBlockEntity = worldIn.getBlockEntity(pos) as PhysicsInfuserBlockEntity
                    return if (te.inventory[0].isEmpty) {
                        te.inventory[0] = player.getItemInHand(handIn).copy()
                        player.getItemInHand(handIn).shrink(1)
                        InteractionResult.SUCCESS
                    } else {
                        InteractionResult.FAIL
                    }
                }
            }
        }
        return InteractionResult.PASS
    }

    // Voxelshape Hell
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape {
        return SHAPE
    }

    override fun getBlockEntityClass(): Class<PhysicsInfuserBlockEntity> {
        return PhysicsInfuserBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PhysicsInfuserBlockEntity> {
        return ClockworkBlockEntities.PHYSICS_INFUSER.get()
    }

    companion object {
        private val SHAPE = makeShape()
        fun isInfuser(state: BlockState): Boolean {
            return state.block is PhysicsInfuserBlock
        }

        private fun makeShape(): VoxelShape {
            var shape = Shapes.empty()
            shape = Shapes.join(shape, box(0.0, 11.0, 0.0, 5.0, 16.0, 5.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(0.5, 0.5, 0.5, 15.5, 15.5, 15.5), BooleanOp.OR)
            shape = Shapes.join(shape, box(0.0, 0.0, 0.0, 5.0, 5.0, 5.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(11.0, 0.0, 0.0, 16.0, 5.0, 5.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(11.0, 11.0, 0.0, 16.0, 16.0, 5.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(11.0, 11.0, 11.0, 16.0, 16.0, 16.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(11.0, 0.0, 11.0, 16.0, 5.0, 16.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(0.0, 0.0, 11.0, 5.0, 5.0, 16.0), BooleanOp.OR)
            shape = Shapes.join(shape, box(0.0, 11.0, 11.0, 5.0, 16.0, 16.0), BooleanOp.OR)
            return shape.optimize()
        }
    }
}