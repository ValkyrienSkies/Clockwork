package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

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
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import java.util.function.Consumer

class GimbalBearingBlock(properties: Properties) : BearingBlock(properties), IBE<GimbalBearingBlockEntity>, ICopyableBlock {

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
        tag ?: return null

        if (tag.contains("BearingPosSX")) {
            val x = tag.getDouble("BearingPosSX")
            val y = tag.getDouble("BearingPosSY")
            val z = tag.getDouble("BearingPosSZ")

            var vectorPartner = Vector3d(x, y, z)

            val partnerId = tag.getLong("connectedShip")
            val centerMigrate = centerPositions[partnerId] ?: return null
            vectorPartner = vectorPartner.sub(centerMigrate.first).add(centerMigrate.second)
            val bp = vectorPartner.toMinecraft()
            tag.putDouble("BearingPosSX", bp.x)
            tag.putDouble("BearingPosSY", bp.y)
            tag.putDouble("BearingPosSZ", bp.z)

            tag.putLong("connectedShip", oldShipIdToNewId[partnerId] ?: -1)

            tag.putInt("JointID", -1)
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

        withBlockEntityDo(worldIn, pos, Consumer { be: GimbalBearingBlockEntity ->
            when (be.isRunning) {
                true -> be.disassemble()
                false -> be.assembleNextTick = true
            }
        })
        return InteractionResult.SUCCESS
    }

    override fun onWrenched(state: BlockState?, context: UseOnContext): InteractionResult {
        if (context.level.isClientSide) return super.onWrenched(state, context)
        val be = context.level.getBlockEntity(context.clickedPos) as? GimbalBearingBlockEntity ?: return InteractionResult.FAIL
        if (be.isRunning) return InteractionResult.FAIL
        return super.onWrenched(state, context)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        if (level.isClientSide) return
        val be = level.getBlockEntity(pos) as? GimbalBearingBlockEntity ?: return
        be.onNeighborChanged()
    }

    override fun getBlockEntityClass(): Class<GimbalBearingBlockEntity> = GimbalBearingBlockEntity::class.java
    override fun getBlockEntityType(): BlockEntityType<out GimbalBearingBlockEntity> = ClockworkBlockEntities.GIMBAL_BEARING.get()
    override fun getRotationAxis(state: BlockState): Direction.Axis = state.getValue(FACING).axis

    override fun getShape(state: BlockState, worldIn: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.NORTH  -> box(2.0,2.0,6.0,14.0,14.0,16.0)
            Direction.SOUTH -> box(2.0,2.0,0.0,14.0,14.0,10.0)
            Direction.EAST -> box(0.0,2.0,2.0,10.0,14.0,14.0)
            Direction.WEST -> box(6.0,2.0,2.0,16.0,14.0,14.0)
            Direction.UP -> box(2.0,0.0,2.0,14.0,10.0,14.0)
            Direction.DOWN -> box(2.0,6.0,2.0,14.0,16.0,14.0)
        }
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face.axis == state.getValue(FACING).axis
    }
}
