package org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.common.assembly.ICopyableBlock

class SpinoffBearingBlock(properties: Properties) : DirectionalBlock(properties), IBE<SpinoffBearingBlockEntity>, ICopyableBlock {

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val facing = if (context.isSecondaryUseActive) {
            context.nearestLookingDirection
        } else {
            context.nearestLookingDirection.opposite
        }
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape? {
        return ClockworkShapes.SPINOFF_BEARING.get(
            state.getValue(FACING)
        )
    }

    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return ClockworkShapes.SPINOFF_BEARING.get(
            state.getValue(FACING)
        )
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        IBE.onRemove(state, level, pos, newState)
        //super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun getBlockEntityClass(): Class<SpinoffBearingBlockEntity> {
        return SpinoffBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SpinoffBearingBlockEntity> {
        return ClockworkBlockEntities.SPINOFF_BEARING.get()
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
        tag ?: return null

        if (tag.contains("partnerX") && tag.contains("partnerY") && tag.contains("partnerZ")) {
            val x = tag.getInt("partnerX")
            val y = tag.getInt("partnerY")
            val z = tag.getInt("partnerZ")
            var vectorPartner = Vector3d(x.toDouble(), y.toDouble(), z.toDouble()).add(0.5, 0.5, 0.5)

            val partnerId = tag.getInt("partnerShipId").toLong()
            val centerMigrate = centerPositions[partnerId] ?: return null
            vectorPartner = vectorPartner.sub(centerMigrate.first).add(centerMigrate.second)
            val bp = vectorPartner.floor().toMinecraft()
            tag.putInt("partnerX", bp.x)
            tag.putInt("partnerY", bp.y)
            tag.putInt("partnerZ", bp.z)

            tag.putBoolean("overrideStatic", true)

            tag.putInt("jointId", -1)
            return tag
        }

        return null
    }
}
