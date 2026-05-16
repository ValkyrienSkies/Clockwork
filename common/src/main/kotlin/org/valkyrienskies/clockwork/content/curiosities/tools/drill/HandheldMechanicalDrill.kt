package org.valkyrienskies.clockwork.content.curiosities.tools.drill

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.simibubi.create.content.equipment.armor.BacktankUtil
import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.stats.Stats
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.PickaxeItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.lwjgl.system.NonnullDefault
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.platform.PlatformUtils.getReachAttribute
import org.valkyrienskies.clockwork.util.BoundingBoxHelper
import java.util.function.Consumer
import kotlin.math.ceil

@NonnullDefault
class HandheldMechanicalDrill(tier: Tier?, attackBonus: Int, attackSpeedBonus: Float, properties: Properties?) :
    PickaxeItem(
        tier,
        attackBonus,
        attackSpeedBonus,
        properties
    ), CustomArmPoseItem {
    override fun mineBlock(
        tool: ItemStack,
        level: Level,
        block: BlockState,
        pos: BlockPos,
        player: LivingEntity
    ): Boolean {
        val success = super.mineBlock(tool, level, block, pos, player)

        if (success && isCorrectToolForDrops(block) && !level!!.isClientSide) {
            if (player!!.isCrouching()) {
                mineTunnel(level, pos, player, CROUCH_MINE_DIAMETER, CROUCH_MINE_DEPTH)
            } else {
                mineTunnel(level, pos, player, MINE_DIAMETER, MINE_DEPTH)
            }
        }

        return success
    }

    override fun isCorrectToolForDrops(block: BlockState): Boolean {
        val ret = block.`is`(BlockTags.MINEABLE_WITH_PICKAXE) || block.`is`(BlockTags.MINEABLE_WITH_SHOVEL) || super.isCorrectToolForDrops(block)
        //println(ret)
        return ret
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
        return if (isCorrectToolForDrops(state)) {
            this.speed
        } else {
            1.0f
        }
    }

    /**
     * Mines a tunnel in the `direction` the player is facing at the given block.<br></br>
     * The tunnel is a rectangular prism of width and height `diameter`, and depth `depth`.
     *
     * @param level    level to break the tunnel in
     * @param origin   position to start the tunnel at
     * @param player   player to simulate breaking the tunnel with
     * @param diameter diameter of the tunnel
     * @param depth    depth of the tunnel
     */
    private fun mineTunnel(level: Level, origin: BlockPos, player: LivingEntity, diameter: Int, depth: Int) {
        val direction = determineDirection(player!!)

        val offsetX = ceil((-diameter / 2f).toDouble()).toInt()
        val offsetY = ceil((-diameter / 2f).toDouble()).toInt()
        val offsetZ = 0

        val boundWidth = diameter
        val boundHeight = diameter
        val boundDepth = depth

        val boundingBox = BoundingBoxHelper.orientBox(
            origin,
            offsetX,
            offsetY,
            offsetZ,
            boundWidth,
            boundHeight,
            boundDepth,
            direction
        )

        BlockPos.betweenClosedStream(boundingBox).forEach { testPos: BlockPos? -> tryMine(level!!, testPos, player) }
    }

    /**
     * Returns the player direction, with added logic for Up and Down based on `MINING_ANGLE_LIMIT`
     *
     * @param player player direction to check
     *
     * @return player direction
     */
    private fun determineDirection(player: LivingEntity): Direction {
        val xRot = player.getXRot()
        if (xRot <= -MINING_ANGLE_LIMIT) {
            return Direction.UP
        } else if (xRot >= MINING_ANGLE_LIMIT) {
            return Direction.DOWN
        } else {
            return player.getDirection()
        }
    }

    /**
     * Attempts to simulate mining a block. If a tool is specified, it must be breakable by that tool
     *
     * @param level        level to break in
     * @param pos          position to break at
     * @param livingEntity entity to simulate the break as (can be `null`)
     */
    private fun tryMine(
        level: Level, pos: BlockPos?,
        livingEntity: LivingEntity?
    ) {
        val minedBlockState = level.getBlockState(pos)

        if (livingEntity != null) {
            val tool = livingEntity.getMainHandItem()

            if (tool.`is`(ClockworkItems.HANDHELD_DRILL.get())) {
                val toolItem = tool.item as HandheldMechanicalDrill
                if (!toolItem.isCorrectToolForDrops(minedBlockState)) {
                    return
                }
            }

            if (livingEntity is Player && livingEntity.hasCorrectToolForDrops(minedBlockState)) {
                livingEntity.awardStat(Stats.BLOCK_MINED.get(minedBlockState.getBlock()))
            }
        }

        if (level.removeBlock(pos, false)) {
            minedBlockState.getBlock().destroy(level, pos, minedBlockState)
        } else {
            return
        }

        if (livingEntity == null) return
        Block.dropResources(minedBlockState, level, pos, null, livingEntity, livingEntity.getMainHandItem())
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
        // Constants
        protected const val MAX_DAMAGE: Int = 2048
        protected const val MAX_BACKTANK_USES: Int = 1000

        // Mining Constants
        private const val MINING_ANGLE_LIMIT = 55

        private const val MINE_DIAMETER = 3
        private const val MINE_DEPTH = 1

        private const val CROUCH_MINE_DIAMETER = 1
        private const val CROUCH_MINE_DEPTH = 3
    }
}
