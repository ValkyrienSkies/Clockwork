package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.AllItems
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Consumer

class SequencedSeatBlock(properties: Properties) : HorizontalKineticBlock(properties),
    IBE<SequencedSeatBlockEntity> {
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val preferredFacing = getPreferredHorizontalFacing(context)
        return if (preferredFacing != null && (context.player == null || !context.player!!.isShiftKeyDown)) withDirection(
            preferredFacing
        ) else withDirection(context.horizontalDirection.opposite)
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val held = player.mainHandItem
        if (AllItems.WRENCH.isIn(held)) return InteractionResult.PASS
        if (held.item is BlockItem) {
            if ((held.item as BlockItem).block is KineticBlock && hasShaftTowards(
                    level,
                    pos,
                    state,
                    hit.direction
                )
            ) return InteractionResult.PASS
        }
        if (player.isShiftKeyDown) {
            if (level.isClientSide) withBlockEntityDo(level, pos) { te: SequencedSeatBlockEntity ->
                displayScreen(
                    te,
                    player
                )
            }
        } else {
            sitDown(level, pos, player)
        }
        return InteractionResult.SUCCESS
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face != Direction.UP
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return Direction.Axis.Y
    }

    private fun withDirection(direction: Direction): BlockState {
        return defaultBlockState().setValue(HORIZONTAL_FACING, direction)
    }

    @Environment(value = EnvType.CLIENT)
    protected fun displayScreen(te: SequencedSeatBlockEntity, player: Player) {
        if (player is LocalPlayer) ScreenOpener.open(SequencedSeatScreen(te))
    }

    override fun getBlockEntityClass(): Class<SequencedSeatBlockEntity> {
        return SequencedSeatBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SequencedSeatBlockEntity> {
        return ClockworkBlockEntities.COMMAND_SEAT.get()
    }

    companion object {
        fun sitDown(level: Level, pos: BlockPos, entity: Entity) {
            if (level.isClientSide) return
            val seat: SequencedSeatEntity = SequencedSeatEntity.create(level, pos)
            seat.setPos(pos.x + .5, pos.y + .4, pos.z + .5)
            level.addFreshEntity(seat)
            entity.startRiding(seat, true)
            if (entity is TamableAnimal) entity.isInSittingPose = true
        }
    }
}
