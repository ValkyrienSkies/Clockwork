package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import java.util.function.Supplier

class WheelHubBlock(properties: Properties) : BearingBlock(properties), IBE<WheelHubBlockEntity>, ICopyableBlock {
    override fun getBlockEntityClass() = WheelHubBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.WHEEL_HUB.get()
    override fun getRenderShape(state: BlockState) = RenderShape.ENTITYBLOCK_ANIMATED
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        SHAPES.getValue(state.getValue(FACING))

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand,
                     hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild() || !player.getItemInHand(hand).isEmpty || player.isShiftKeyDown) return InteractionResult.PASS
        val hub = level.getBlockEntity(pos) as? WheelHubBlockEntity ?: return InteractionResult.PASS
        if (!level.isClientSide) hub.toggleDrive(player)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity
        if (hub?.wheel != null) return InteractionResult.PASS
        return super.onWrenched(state, context)
    }

    override fun onSneakWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity
        if (hub?.wheel == null || context.clickedFace != hub.facing) return super.onSneakWrenched(state, context)
        if (!context.level.isClientSide) hub.uninstall(context.player)
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, be: BlockEntity?,
                        shipsBeingCopied: List<ServerShip>, centerPositions: Map<Long, Vector3dc>): CompoundTag? {
        (be as? WheelHubBlockEntity)?.prepareMove(level)
        return be?.saveWithFullMetadata()?.also(::clearBodyReference)
    }

    override fun onPaste(level: ServerLevel, pos: BlockPos, state: BlockState, oldShipIdToNewId: Map<Long, Long>,
                         centerPositions: Map<Long, Pair<Vector3dc, Vector3dc>>, oldJointIdToNewId: Map<Int, Int>,
                         tag: CompoundTag?) = tag

    override fun pasteSurvivalCost(data: Supplier<CompoundTag>?): Map<Item, Int>? {
        val size = data?.get()?.getString("Wheel")?.let(WheelSize::byId) ?: return null
        return mapOf(item(size) to 1)
    }

    companion object {
        private val SHAPES = Direction.entries.associateWith { facing ->
            val low = doubleArrayOf(5.0, 5.0, 5.0)
            val high = doubleArrayOf(11.0, 11.0, 11.0)
            low[facing.axis.ordinal] = if (facing.axisDirection == Direction.AxisDirection.POSITIVE) 0.0 else 8.0
            high[facing.axis.ordinal] = if (facing.axisDirection == Direction.AxisDirection.POSITIVE) 8.0 else 16.0
            box(low[0], low[1], low[2], high[0], high[1], high[2])
        }
        fun clearBodyReference(tag: CompoundTag) {
            for (key in listOf("WheelBody", "WheelJoint", "WheelOwner", "WheelParent", "WheelDimension")) tag.remove(key)
        }

        fun item(size: WheelSize): Item = when (size) {
            WheelSize.SMALL -> ClockworkItems.SMALL_WHEEL.get()
            WheelSize.MEDIUM -> ClockworkItems.MEDIUM_WHEEL.get()
            WheelSize.LARGE -> ClockworkItems.LARGE_WHEEL.get()
        }
    }
}
