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
import org.valkyrienskies.clockwork.AreaData
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.CWItem
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
                areaData.firstPos = Optional.empty()
                areaData.secondPos = Optional.empty()
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
        if (!stack.`is`(this)) {
            return super.useOn(context)
        }
        val areaData = AreaData.of(player).get()

        if (areaData.firstPos.isEmpty) {
            areaData.firstPos = Optional.of(pos)
            player.displayClientMessage(
                Component.literal("First Position Selected!").withStyle(
                    Style.EMPTY.withColor(
                        ChatFormatting.DARK_PURPLE
                    )
                ), true
            )
            player.cooldowns.addCooldown(this, 10)
            ClockworkPackets.sendToClientsTrackingAndSelf(BluperGluePacket(areaData.firstPos), player as ServerPlayer)
            return InteractionResult.SUCCESS
        } else if (areaData.secondPos.isEmpty && areaData.firstPos.isPresent) {
            areaData.secondPos = Optional.of(pos)
            if (areaData.firstPos.get().distSqr(areaData.secondPos.get()) > 500) {
                player.displayClientMessage(
                    Component.literal("Area Too Large!").withStyle(
                        Style.EMPTY.withColor(
                            ChatFormatting.DARK_PURPLE
                        )
                    ), true
                )
                areaData.firstPos = Optional.empty()
                areaData.secondPos = Optional.empty()
                return InteractionResult.SUCCESS
            }
            val area: AABBic = AABBi(
                min(areaData.firstPos.get().x, areaData.secondPos.get().x),
                min(areaData.firstPos.get().y, areaData.secondPos.get().y),
                min(areaData.firstPos.get().z, areaData.secondPos.get().z),
                max(areaData.firstPos.get().x, areaData.secondPos.get().x),
                max(areaData.firstPos.get().y, areaData.secondPos.get().y),
                max(areaData.firstPos.get().z, areaData.secondPos.get().z)
            )
            areaData.firstPos = Optional.empty()
            areaData.secondPos = Optional.empty()
            val selectedArea = AreaData.of(player).get().area
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
            data.area = selectedArea

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