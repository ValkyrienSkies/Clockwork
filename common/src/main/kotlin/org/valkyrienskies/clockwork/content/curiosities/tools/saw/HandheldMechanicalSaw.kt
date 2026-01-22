package org.valkyrienskies.clockwork.content.curiosities.tools.saw

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.simibubi.create.content.equipment.armor.BacktankUtil
import com.simibubi.create.content.kinetics.saw.TreeCutter
import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.createmod.catnip.math.VecHelper
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Tier
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.lwjgl.system.NonnullDefault
import org.valkyrienskies.clockwork.platform.PlatformUtils.getReachAttribute
import java.util.function.BiConsumer
import java.util.function.Consumer

@NonnullDefault
class HandheldMechanicalSaw(tier: Tier?, attackBonus: Float, attackSpeedBonus: Float, properties: Properties?) :
    AxeItem(
        tier,
        attackBonus,
        attackSpeedBonus,
        properties
    ), CustomArmPoseItem {
    override fun mineBlock(
        tool: ItemStack?,
        level: Level?,
        block: BlockState?,
        pos: BlockPos?,
        livingEntity: LivingEntity?
    ): Boolean {
        val ret = super.mineBlock(tool, level, block, pos, livingEntity)

        if (livingEntity!!.isCrouching()) return ret

        level!!.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())

        val dynamicTree = TreeCutter.findDynamicTree(block!!.getBlock(), pos)
        if (dynamicTree.isPresent()) {
            dynamicTree.get()
                .destroyBlocks(
                    level,
                    livingEntity,
                    BiConsumer { blockPos: BlockPos?, stack: ItemStack? ->
                        dropItemFromCutTree(
                            level,
                            blockPos,
                            stack
                        )
                    })
            return true
        }

        val player = if (livingEntity is Player) livingEntity else null
        TreeCutter.findTree(level, pos, block)
            .destroyBlocks(
                level,
                tool,
                player,
                BiConsumer { blockPos: BlockPos?, drop: ItemStack? -> dropItemFromCutTree(level, blockPos, drop) })
        return ret
    }

    fun dropItemFromCutTree(level: Level?, blockPos: BlockPos?, drop: ItemStack?) {
        val dropPos = VecHelper.getCenterOf(blockPos)
        val entity = ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, drop)
        level!!.addFreshEntity(entity)
    }

    override fun getDefaultAttributeModifiers(slot: EquipmentSlot?): Multimap<Attribute?, AttributeModifier?>? {
        val attributes = ImmutableMultimap.Builder<Attribute?, AttributeModifier?>()
        val distanceAttribute = getReachAttribute()
        attributes.putAll(super.getDefaultAttributeModifiers(slot))
        //attributes.put(distanceAttribute, AttributeModifier("Reach", -3.0, AttributeModifier.Operation.ADDITION))
        return attributes.build()
    }

    override fun isBarVisible(stack: ItemStack?): Boolean {
        return BacktankUtil.isBarVisible(stack, MAX_BACKTANK_USES)
    }

    override fun getBarWidth(stack: ItemStack?): Int {
        return BacktankUtil.getBarWidth(stack, MAX_BACKTANK_USES)
    }

    override fun getBarColor(stack: ItemStack?): Int {
        return BacktankUtil.getBarColor(stack, MAX_BACKTANK_USES)
    }

    override fun getUseAnimation(stack: ItemStack?): UseAnim? {
        return UseAnim.NONE
    }

    fun <T : LivingEntity?> damageItem(stack: ItemStack?, amount: Int, entity: T?, onBroken: Consumer<T?>?): Int {
        if (BacktankUtil.canAbsorbDamage(entity, MAX_BACKTANK_USES)) {
            return 0
        }
        return amount
    }

    override fun getArmPose(
        stack: ItemStack?,
        player: AbstractClientPlayer,
        hand: InteractionHand?
    ): HumanoidModel.ArmPose? {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD
    }

    companion object {
        const val MAX_DAMAGE: Int = 2048
        const val MAX_BACKTANK_USES: Int = 1000
    }
}
