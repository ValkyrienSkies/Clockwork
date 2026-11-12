package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos

class VdexBlueprintItem(properties: Properties) : CWItem(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (context.hand != InteractionHand.MAIN_HAND || !player.mayBuild()) return InteractionResult.PASS
        val stack = context.itemInHand
        val ship = context.level.getShipManagingPos(context.clickedPos)
        if (!context.level.isClientSide) {
            player as ServerPlayer
            if (ship is ServerShip && (player.isShiftKeyDown || !stack.orCreateTag.contains("VdexFile"))) {
                VdexBlueprintLibrary.capture(player, ship, stack)
            } else if (stack.tag?.contains("VdexFile") == true && ship == null) {
                VdexBlueprintLibrary.guarded(player) {
                    val blueprint = VdexBlueprintLibrary.load(player.serverLevel(), stack.tag!!.getString("VdexFile"))
                    val position = Vec3.atBottomCenterOf(context.clickedPos.relative(context.clickedFace)).add(0.0, -blueprint.bounds.minY, 0.0)
                    VdexBlueprintLibrary.deploy(player, stack, position, stack.tag!!.getFloat("VdexYaw"))
                }
            }
        }
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand))
        if (level.isClientSide) VdexBlueprintClient.open()
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flag: TooltipFlag) {
        stack.tag?.getString("VdexName")?.takeIf { it.isNotBlank() }?.let { lines.add(Component.literal(it).withStyle(ChatFormatting.AQUA)) }
        if (stack.tag?.getBoolean("Deployed") == true) lines.add(Component.translatable("item.vs_clockwork.vdex_blueprint.deployed").withStyle(ChatFormatting.GRAY))
        super.appendHoverText(stack, level, lines, flag)
    }
}
