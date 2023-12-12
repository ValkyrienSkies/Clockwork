package org.valkyrienskies.clockwork.content.curiosities.tools.bluper

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.AreaData
import org.valkyrienskies.mod.common.isBlockInShipyard
import java.util.*
import kotlin.math.max
import kotlin.math.min

class BluperGlueItem(properties: Properties) : CWItem(properties) {
    private var wasSelected = false
    private var shouldRenderOutlines = false

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)

        if (level.isClientSide) {
            return
        }

        if (isSelected && !this.wasSelected) {
            this.shouldRenderOutlines = true
        } else if (!isSelected && this.wasSelected) {
            this.shouldRenderOutlines = false
        }
        this.wasSelected = isSelected

        if (!isSelected) {
            if (entity is Player) {
                val areaData = AreaData.of(entity).get()
                areaData.setFirstPos(Optional.empty())
                areaData.setSecondPos(Optional.empty())
            }
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.FAIL
        val world = context.level
        if (world.isClientSide) {
            return InteractionResult.PASS
        }

        val hand = context.hand
        val stack = player.getItemInHand(hand)
        val pos: BlockPos = context.clickedPos
        if (!stack.`is`(this) || world.isBlockInShipyard(pos)) {
            return super.useOn(context)
        }
        val areaData = AreaData.of(player).get()

        if (areaData.getFirstPos().isEmpty) {
            areaData.setFirstPos(Optional.of(pos))
            player.displayClientMessage(
                Component.literal("First Position Selected!").withStyle(
                    Style.EMPTY.withColor(
                        ChatFormatting.DARK_PURPLE
                    )
                ), true
            )
            player.cooldowns.addCooldown(this, 10)
            ClockworkPackets.sendToClientsTrackingAndSelf(
                BluperGluePacket(areaData.getFirstPos()),
                player as ServerPlayer
            )
            return InteractionResult.SUCCESS
        } else if (areaData.getSecondPos().isEmpty && areaData.getFirstPos().isPresent) {
            areaData.setSecondPos(Optional.of(pos))
            if (areaData.getFirstPos().get().distSqr(areaData.getSecondPos().get()) > 500) {
                player.displayClientMessage(
                    Component.literal("Area Too Large!").withStyle(
                        Style.EMPTY.withColor(
                            ChatFormatting.DARK_PURPLE
                        )
                    ), true
                )
                areaData.setFirstPos(Optional.empty())
                areaData.setSecondPos(Optional.empty())
                return InteractionResult.SUCCESS
            }
            val area: AABBic = AABBi(
                min(areaData.getFirstPos().get().x, areaData.getSecondPos().get().x),
                min(areaData.getFirstPos().get().y, areaData.getSecondPos().get().y),
                min(areaData.getFirstPos().get().z, areaData.getSecondPos().get().z),
                max(areaData.getFirstPos().get().x, areaData.getSecondPos().get().x),
                max(areaData.getFirstPos().get().y, areaData.getSecondPos().get().y),
                max(areaData.getFirstPos().get().z, areaData.getSecondPos().get().z)
            )
            areaData.setFirstPos(Optional.empty())
            areaData.setSecondPos(Optional.empty())
            val selectedArea = AreaData.of(player).get().getArea()
            if (selectedArea.containsAABB(area)) {
                player.displayClientMessage(
                    Component.literal("Area Already Exists.").withStyle(
                        Style.EMPTY.withColor(
                            ChatFormatting.DARK_PURPLE
                        )
                    ), true
                )
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }

            if (selectedArea.selectedAreas.size >= 150) {
                player.displayClientMessage(
                    Component.literal("At selection capacity.").withStyle(
                        Style.EMPTY.withColor(
                            ChatFormatting.DARK_PURPLE
                        )
                    ), true
                )
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }

            if (selectedArea.selectionClusters.size > 1) {
                areaData.shouldReset(true)
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }
            selectedArea.clusterNewArea(area)

            val data = AreaData.of(player).get()
            data.setArea(selectedArea)

            player.displayClientMessage(
                Component.literal("Area Created!").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)),
                true
            )
            stack.damageValue = stack.damageValue - 1
            player.cooldowns.addCooldown(this, 10)
            return InteractionResult.SUCCESS
        }
        return super.useOn(context)
    }
}