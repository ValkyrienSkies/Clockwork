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
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d
import java.util.function.Consumer

class PhysBearingBlock(properties: Properties) : BearingBlock(properties), IBE<PhysBearingBlockEntity>, ICopyableBlock {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, be: BlockEntity?, shipsBeingCopied: List<ServerShip>, centerPositions: Map<Long, Vector3dc>): CompoundTag? = null
    override fun onPaste(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        oldShipIdToNewId: Map<Long, Long>,
        centerPositions: Map<Long, Pair<Vector3dc, Vector3dc>>,
        tag: CompoundTag?
    ): CompoundTag? {
        tag ?: return null

        if (tag.contains("partnerPosx")) {
            var newPartnerPos = tag.getVector3d("partnerPos")!!

            val partnerId = tag.getInt("partnerShipId").toLong()
            val centerMigrate = centerPositions[partnerId] ?: return null
            newPartnerPos = newPartnerPos.sub(centerMigrate.first).add(centerMigrate.second)
            tag.putVector3d("partnerPos", newPartnerPos)
            tag.putInt("jointId", -1)
            return tag
        }

        return null
    }

    override fun use(state: BlockState, worldIn: Level, pos: BlockPos, player: Player, handIn: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (handIn == InteractionHand.OFF_HAND) return InteractionResult.FAIL
        if (!player.getItemInHand(handIn).isEmpty) return InteractionResult.PASS
        if (worldIn.isClientSide) return InteractionResult.SUCCESS

        withBlockEntityDo(worldIn, pos, Consumer withBlockEntityDo@{ te: PhysBearingBlockEntity ->
            if (te.jointId != -1) te.disassemble() else te.assemble()
        })
        return InteractionResult.SUCCESS
    }

    override fun onWrenched(state: BlockState?, context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return super.onWrenched(state, context)
        val be = context.getLevel().getBlockEntity(context.getClickedPos()) as? PhysBearingBlockEntity ?: return InteractionResult.FAIL
        if (be.jointId != -1 && !ClockworkConfig.SERVER.allowWrenchingActivatedPhysBearing) return InteractionResult.FAIL

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
        fun getLight(state: BlockState?): Int {
            return 11
        }
    }
}
