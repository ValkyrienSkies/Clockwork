package org.valkyrienskies.clockwork.content.contraptions.phys.infuser


import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem

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
        if (!player.mayBuild()) return InteractionResult.FAIL

        val blockEntity = worldIn.getBlockEntity(pos) as? PhysicsInfuserBlockEntity
            ?: return InteractionResult.PASS

        // 1. Lock interactions while assembling animation is active
        if (blockEntity.assembling) {
            if (!worldIn.isClientSide) blockEntity.skipAssembly()
            return InteractionResult.sidedSuccess(worldIn.isClientSide)
        }

        val heldStack = player.getItemInHand(handIn)
        val wandStack = blockEntity.inventory[0]
        val hasWand = !wandStack.isEmpty
        val isShift = player.isShiftKeyDown

        // 2. Shift + Right Click: Remove Wand (if wand present)
        if (isShift && hasWand) {
            if (!worldIn.isClientSide) {
                player.inventory.placeItemBackInInventory(wandStack.copy())
                blockEntity.inventory[0] = net.minecraft.world.item.ItemStack.EMPTY
                blockEntity.setChanged()
            }
            return InteractionResult.sidedSuccess(worldIn.isClientSide)
        }

        // 3. Right Click with WanderwandItem: Insert Wand (if empty)
        if (heldStack.item is WanderwandItem && !hasWand) {
            if (!worldIn.isClientSide) {
                blockEntity.inventory[0] = heldStack.split(1)
                blockEntity.setChanged()
            }
            return InteractionResult.sidedSuccess(worldIn.isClientSide)
        }

        // 4. Right Click with Empty Hand: Start Assembly Sequence (if wand present)
        if (hasWand && heldStack.isEmpty) {
            if (!worldIn.isClientSide) {
                blockEntity.startAssembly(player as? ServerPlayer)
            }
            return InteractionResult.sidedSuccess(worldIn.isClientSide)
        }

        return InteractionResult.PASS
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        movedByPiston: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (level.isClientSide) return
        val blockEntity = level.getBlockEntity(pos) as? PhysicsInfuserBlockEntity ?: return
        blockEntity.redstoneChanged()
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        if (!state.`is`(next.block)) {
            if (!level.isClientSide && !moved) {
                val be = level.getBlockEntity(pos) as? PhysicsInfuserBlockEntity
                if (be != null) Containers.dropContents(level, pos, be)
            }
            super.onRemove(state, level, pos, next, moved)
        }
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
    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    companion object {
        private val SHAPE = makeShape()
        fun isInfuser(state: BlockState): Boolean {
            return state.block is PhysicsInfuserBlock
        }

        private fun makeShape(): VoxelShape {
            return box(1.0,1.0,1.0,15.0,15.0,15.0)
        }
    }
}
