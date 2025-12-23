package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.platform.CWItem

class GravitronItem(properties: Properties) : CWItem(properties), CustomArmPoseItem {

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        if (entity !is Player || level !is ServerLevel) {
            return
        }

        // Make sure we only tick for the gravitrons
        if (!stack.`is`(ClockworkItems.GRAVITRON.get().asItem())) return


        val isHoldingGravitron = entity.mainHandItem.`is`(ClockworkItems.GRAVITRON.get().asItem())
        val isHoldingCreativeGravitron = entity.mainHandItem.`is`(ClockworkItems.CREATIVE_GRAVITRON.get().asItem())

        if (!(isHoldingGravitron || isHoldingCreativeGravitron)) {
            GrabTool.dropShip(entity)

            if (stack.tag != null) {
                if (stack.tag!!.contains("ShipId")) {
                    stack.tag!!.remove("ShipId")
                }
                if (stack.tag!!.contains("GrabbedPosInShip")) {
                    stack.tag!!.remove("GrabbedPosInShip")
                }
            }
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.NONE
    }

    override fun getArmPose(stack: ItemStack?, player: AbstractClientPlayer, hand: InteractionHand?): HumanoidModel.ArmPose? {
        if (!player.swinging) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD
        }
        return null
    }

    override fun canAttackBlock(state: BlockState, world: Level, pos: BlockPos, player: Player): Boolean {
        return false
    }
}
