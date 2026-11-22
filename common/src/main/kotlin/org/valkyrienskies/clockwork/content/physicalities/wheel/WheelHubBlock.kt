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
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.CollisionContext
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.api.wheel.TireTypes
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.screwdriver.IScrewdrivable
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import java.util.function.Supplier

class WheelHubBlock(properties: Properties) : BearingBlock(properties), IBE<WheelHubBlockEntity>, ICopyableBlock, IScrewdrivable {
    override fun getBlockEntityClass() = WheelHubBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.WHEEL_HUB.get()
    override fun getRenderShape(state: BlockState) = RenderShape.ENTITYBLOCK_ANIMATED
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        val size = (level.getBlockEntity(pos) as? WheelHubBlockEntity)?.wheel ?: return SHAPES.getValue(facing)
        return FITTED_SHAPES.getValue(facing).getValue(size)
    }

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        SHAPES.getValue(state.getValue(FACING))

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand,
                     hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild() || !player.getItemInHand(hand).isEmpty || player.isShiftKeyDown) return InteractionResult.PASS
        if (hand == InteractionHand.MAIN_HAND && player.offhandItem.item is AirPumpItem) return InteractionResult.PASS
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
        if (hub?.wheel != null) return InteractionResult.PASS
        return super.onSneakWrenched(state, context)
    }

    override fun onScrewdrived(state: BlockState, context: UseOnContext) = InteractionResult.PASS

    override fun onSneakScrewdrived(state: BlockState, context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity
        if (hub?.wheel == null || context.clickedFace != hub.facing) return InteractionResult.PASS
        val mainItem = context.player?.mainHandItem?.item
        if (context.hand == InteractionHand.OFF_HAND && (mainItem is WheelItem || mainItem is TireItem))
            return InteractionResult.PASS
        if (!context.level.isClientSide) {
            if (hub.tireType != null && hub.tireType != TireType.WOOD) hub.removeTire(context.player)
            else hub.uninstall(context.player)
        }
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
        val tag = data?.get() ?: return null
        val size = WheelSize.byId(tag.getString("Wheel")) ?: return null
        return parts(size, TireType.read(tag)).associateWith { 1 }
    }

    companion object {
        private val SHAPES = Direction.entries.associateWith { facing ->
            val low = doubleArrayOf(5.0, 5.0, 5.0)
            val high = doubleArrayOf(11.0, 11.0, 11.0)
            low[facing.axis.ordinal] = if (facing.axisDirection == Direction.AxisDirection.POSITIVE) 0.0 else 8.0
            high[facing.axis.ordinal] = if (facing.axisDirection == Direction.AxisDirection.POSITIVE) 8.0 else 16.0
            box(low[0], low[1], low[2], high[0], high[1], high[2])
        }
        private val FITTED_SHAPES = Direction.entries.associateWith { facing ->
            WheelSize.entries.associateWith { size ->
                val half = DoubleArray(3) { size.rimRadius * 16 }
                half[facing.axis.ordinal] = size.rimWidth * 8
                Shapes.or(SHAPES.getValue(facing), box(8 - half[0], 8 - half[1], 8 - half[2],
                    8 + half[0], 8 + half[1], 8 + half[2]))
            }
        }

        fun clearBodyReference(tag: CompoundTag) {
            for (key in listOf("WheelBody", "WheelJoint", "WheelOwner", "WheelParent", "WheelDimension")) tag.remove(key)
        }

        fun parts(size: WheelSize, type: TireType?): List<Item> =
            if (type == TireType.WOOD) listOf(item(size, type))
            else listOfNotNull(item(size), type?.let(TireTypes::item))

        fun item(size: WheelSize, type: TireType = TireType.RUBBER): Item =
            ClockworkItems.WHEELS.getValue(type).getValue(size).get()
    }
}
