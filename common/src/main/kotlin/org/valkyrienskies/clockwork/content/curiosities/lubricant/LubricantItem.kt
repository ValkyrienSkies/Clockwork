package org.valkyrienskies.clockwork.content.curiosities.lubricant

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import dev.architectury.event.EventResult
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.mod.common.getShipManagingPos

class LubricantItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        val level = context.level as ServerLevel
        val pos = context.clickedPos
        val stack = context.itemInHand
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL
        val tag = stack.orCreateTag
        val dimension = level.dimension().location().toString()
        val ship = level.getShipManagingPos(pos)?.id ?: -1L
        if (!tag.contains("Start") || tag.getString("Dimension") != dimension) {
            tag.putLong("Start", pos.asLong())
            tag.putLong("Ship", ship)
            tag.putString("Dimension", dimension)
            player.displayClientMessage(Component.translatable("item.vs_clockwork.lubricant.start"), true)
            return InteractionResult.CONSUME
        }
        val start = BlockPos.of(tag.getLong("Start"))
        if (ship != tag.getLong("Ship")) {
            player.displayClientMessage(Component.translatable("item.vs_clockwork.lubricant.same_body"), true)
            return InteractionResult.FAIL
        }
        if (!validSize(start, pos)) {
            player.displayClientMessage(Component.translatable("item.vs_clockwork.lubricant.too_large"), true)
            return InteractionResult.FAIL
        }
        val data = LubricantData.get(level)
        val area = ArrayList<BlockPos>()
        var changed = 0
        for (cursor in BlockPos.betweenClosed(start, pos)) {
            if (!level.hasChunkAt(cursor) || !level.mayInteract(player, cursor)) continue
            if ((level.getShipManagingPos(cursor)?.id ?: -1L) != ship) continue
            val state = level.getBlockState(cursor)
            if (state.isAir || state.getCollisionShape(level, cursor).isEmpty) continue
            area.add(cursor.immutable())
            if (data.set(cursor, state.block, true)) {
                LubricantMaterials.refresh(level, cursor, true)
                changed++
            }
        }
        if (changed > 0) data.addArea(area)
        clear(stack)
        if (changed > 0) {
            ClockworkSounds.LUBRICANT_APPLY.playOnServer(level, player.blockPosition(), 0.8f, 0.95f + level.random.nextFloat() * 0.1f)
            stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(context.hand) }
        }
        player.displayClientMessage(Component.translatable("item.vs_clockwork.lubricant.applied", changed), true)
        return InteractionResult.CONSUME
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (player.isShiftKeyDown) {
            if (!level.isClientSide) clear(stack)
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
        }
        return InteractionResultHolder.pass(stack)
    }

    companion object {
        const val MAX_BLOCKS = 4096L

        fun interact(player: Player, hand: InteractionHand, pos: BlockPos, face: Direction): EventResult {
            val item = player.getItemInHand(hand).item as? LubricantItem ?: return EventResult.pass()
            if (player.isSpectator) return EventResult.pass()
            val result = item.useOn(UseOnContext(player, hand, BlockHitResult(Vec3.atCenterOf(pos), face, pos, false)))
            return if (result.consumesAction()) EventResult.interruptTrue() else EventResult.interruptFalse()
        }

        fun validSize(a: BlockPos, b: BlockPos): Boolean {
            val x = kotlin.math.abs(a.x.toLong() - b.x) + 1
            val y = kotlin.math.abs(a.y.toLong() - b.y) + 1
            val z = kotlin.math.abs(a.z.toLong() - b.z) + 1
            return x <= 64 && y <= 64 && z <= 64 && x * y * z <= MAX_BLOCKS
        }

        fun clear(stack: ItemStack) {
            for (key in listOf("Start", "Ship", "Dimension", "Remove")) stack.tag?.remove(key)
        }
    }
}
