package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.AllShapes
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.util.putVector3d
import java.util.function.Consumer

class PhysBearingBlock(properties: Properties) : BearingBlock(properties), IBE<PhysBearingBlockEntity>, ICopyableBlock {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, be: BlockEntity?, shipsBeingCopied: List<ServerShip>, centerPositions: Map<Long, Vector3d>): CompoundTag? = null
    override fun onPaste(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        oldShipIdToNewId: Map<Long, Long>,
        centerPositions: Map<Long, Pair<Vector3d, Vector3d>>,
        tag: CompoundTag?
    ): CompoundTag? {
        val tag = tag ?: return null

        val oldId = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, oldShipIdToNewId[oldId] ?: -1)
        val (oldCenter, newCenter) = centerPositions[oldId] ?: (Vector3d() to Vector3d())
        val oldShiptraptionCenter = tag.getVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER)!!
        tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER, oldShiptraptionCenter.sub(oldCenter).add(newCenter))
        return tag
    }

    override fun use(state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (handIn == InteractionHand.OFF_HAND) return InteractionResult.FAIL
        if (!player.getItemInHand(handIn).isEmpty) return InteractionResult.PASS
        if (worldIn.isClientSide) return InteractionResult.SUCCESS

        withBlockEntityDo(worldIn, pos, Consumer withBlockEntityDo@{ te: PhysBearingBlockEntity ->
            when (te.isRunning) {
                true -> te.disassemble()
                false -> te.assembleNextTick = true
            }
        })
        return InteractionResult.SUCCESS
    }

    override fun onWrenched(state: BlockState?, context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return super.onWrenched(state, context)
        val be = context.getLevel().getBlockEntity(context.getClickedPos()) as? PhysBearingBlockEntity ?: return InteractionResult.FAIL
        if (be.isRunning && !ClockworkConfig.SERVER.allowWrenchingActivatedPhysBearing) return InteractionResult.FAIL

        return super.onWrenched(state, context)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        if (level.isClientSide) {return}
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity !is PhysBearingBlockEntity) {return}
    }

    override fun getBlockEntityClass(): Class<PhysBearingBlockEntity> = PhysBearingBlockEntity::class.java
    override fun getBlockEntityType(): BlockEntityType<out PhysBearingBlockEntity> = ClockworkBlockEntities.PHYS_BEARING.get()
    override fun getRotationAxis(state: BlockState): Direction.Axis = state.getValue(FACING).axis

    override fun getShape(state: BlockState, worldIn: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.MECHANICAL_PISTON[state.getValue(FACING)]
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face == state.getValue(FACING).opposite
    }

    companion object {
        fun getLight(state: BlockState?): Int = 8
    }
}
