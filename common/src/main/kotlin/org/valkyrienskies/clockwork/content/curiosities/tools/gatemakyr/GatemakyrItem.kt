package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.platform.CWItem

class GatemakyrItem(properties: Properties) : CWItem(properties), CustomArmPoseItem {
    override fun canAttackBlock(state: BlockState, world: Level, pos: BlockPos, player: Player) = false
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> =
        InteractionResultHolder.consume(player.getItemInHand(hand))

    override fun getArmPose(stack: ItemStack?, player: AbstractClientPlayer, hand: InteractionHand?) = HumanoidModel.ArmPose.CROSSBOW_HOLD

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flag: TooltipFlag) {
        lines.add(Component.translatable("vs_clockwork.gatemakyr.tooltip"))
    }

    companion object {
        private const val SIZE = "GatemakyrSize"
        const val WIDTH = "GatemakyrWidth"
        const val HEIGHT = "GatemakyrHeight"
        fun size(stack: ItemStack, axis: String): Int =
            (stack.tag?.getInt(axis)?.takeIf { it > 0 } ?: stack.tag?.getInt(SIZE)?.takeIf { it > 0 } ?: 3).coerceIn(1, 32)
        const val FREE = "GatemakyrFree"
        const val DISTANCE = "GatemakyrDistance"
        fun distance(stack: ItemStack): Double = (stack.tag?.getDouble(DISTANCE)?.takeIf { it.isFinite() && it >= 2.0 } ?: 8.0).coerceIn(2.0, 48.0)
    }
}
