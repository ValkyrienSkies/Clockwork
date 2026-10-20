package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.ProperWaterloggedBlock
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.assembly.ICopyableBlock

class UniversalShaftBlock(properties: Properties?) : DirectionalKineticBlock(properties), IBE<UniversalShaftBlockEntity>, ICopyableBlock, ProperWaterloggedBlock {

    init {
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(BlockStateProperties.WATERLOGGED))
    }

    override fun getFluidState(state: BlockState): FluidState {
        return fluidState(state)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return withWater(super.getStateForPlacement(context), context)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighbourState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighbourPos: BlockPos
    ): BlockState {
        updateWater(level, state, pos)
        return state
    }

    override fun onCopy(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        be: BlockEntity?,
        shipsBeingCopied: List<ServerShip>,
        centerPositions: Map<Long, Vector3dc>
    ): CompoundTag? {
        return null
    }

    override fun onPaste(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        oldShipIdToNewId: Map<Long, Long>,
        centerPositions: Map<Long, Pair<Vector3dc, Vector3dc>>,
        tag: CompoundTag?
    ): CompoundTag? {
        if (!(tag?.contains("otherPosX") ?: false)) return tag
        if (!(tag.contains("otherShipId"))) return tag

        val connectedPos = BlockPos(tag.getInt("otherPosX"),tag.getInt("otherPosY"),tag.getInt("otherPosZ"))
        val oldId = tag.getLong("otherShipId")

        val offset = connectedPos.center.toJOML().sub(centerPositions[oldId]?.first ?: return tag)
        val newCenter = centerPositions[oldId]?.second?.add(offset, Vector3d()) ?: return tag

        val newPos = BlockPos.containing(newCenter.toMinecraft())
        tag.putInt("otherPosX", newPos.x)
        tag.putInt("otherPosY", newPos.y)
        tag.putInt("otherPosZ", newPos.z)

        return tag
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(BlockStateProperties.FACING).axis
    }

    override fun getBlockEntityClass(): Class<UniversalShaftBlockEntity> {
        return UniversalShaftBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out UniversalShaftBlockEntity> {
        return ClockworkBlockEntities.UNIVERSAL_SHAFT.get()
    }

    override fun getShape(
        state: BlockState,
        worldIn: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return ClockworkShapes.UNIVERSAL_SHAFT.get(state.getValue<Direction>(FACING).opposite)
    }

    override fun getRenderShape(pState: BlockState?): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face == state.getValue(BlockStateProperties.FACING)
    }

    override fun getPreferredFacing(context: BlockPlaceContext): Direction? {
        var prefferedSide: Direction? = null
        for (side in Iterate.directions) {
            val blockState = context.getLevel()
                .getBlockState(
                    context.getClickedPos()
                        .relative(side)
                )
            if (blockState.getBlock() is IRotate) {
                if ((blockState.getBlock() as IRotate).hasShaftTowards(
                        context.getLevel(), context.getClickedPos()
                            .relative(side), blockState, side.getOpposite()
                    )
                ) if (prefferedSide != null && prefferedSide.getAxis() !== side.getAxis()) {
                    prefferedSide = null
                    break
                } else {
                    prefferedSide = side.opposite
                }
            }
        }
        return prefferedSide
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? UniversalShaftBlockEntity ?: return InteractionResult.PASS
        if (player.isShiftKeyDown && player.getItemInHand(InteractionHand.MAIN_HAND) == ItemStack.EMPTY) {
            if (be.connectedBe != null) be.connectedBe!!.disconnect()
            be.disconnect()
            level.playSound(null, be.blockPos, ClockworkSounds.HOSE_RELEASE.mainEvent, SoundSource.BLOCKS, 1.0f, 1.0f)
            if (!player.isCreative) player.addItem(be.getConnectionItem())
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    override fun getDrops(
        state: BlockState,
        params: LootParams.Builder
    ): List<ItemStack> {
        val drops = super.getDrops(state, params).toMutableList()

        val blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)

        if (blockEntity is UniversalShaftBlockEntity && (blockEntity.connectedBe != null)) {
            drops.add(ItemStack(ClockworkItems.UNIVERSAL_SHAFT_ITEM.get()))
        }

        return drops
    }
}
