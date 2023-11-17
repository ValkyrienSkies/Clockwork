package org.valkyrienskies.clockwork.util.blocktype

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.Color
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem

abstract class DyedWing(properties: Properties?) :
    ConnectedWingAlike(properties), IBE<ColorBlockEntity> {
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        val be: ColorBlockEntity = (level.getBlockEntity(pos) as ColorBlockEntity)
        val color: Int = be.getColor()
        if (stack.item is DyeItem && color != (stack.item as DyeItem).getDyeColor().getTextColor()) {
            val dye = stack.item as DyeItem
            be.setColor(
                if (color == -1) dye.getDyeColor().getTextColor() else Color.mixColors(
                    color,
                    dye.getDyeColor().getTextColor(),
                    0.5f
                )
            )
            if (!level.isClientSide && !player.isCreative) {
                if (stack.count > 1) stack.shrink(1) else if (stack.count == 1) player.setItemInHand(
                    hand,
                    ItemStack.EMPTY
                )
            }
            return InteractionResult.SUCCESS
        }
        return super.use(state, level, pos, player, hand, hit)
    }

    override fun getBlockEntityClass(): Class<ColorBlockEntity> {
        return ColorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ColorBlockEntity> {
        return ClockworkBlockEntities.COLOR_BLOCK_ENTITY.get()
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> {
        val drops = super.getDrops(state, params)
        drops.replaceAll { stack: ItemStack ->
            val be: ColorBlockEntity =
                params.getParameter<BlockEntity>(LootContextParams.BLOCK_ENTITY) as ColorBlockEntity
            val color: Int = be.getColor()
            if (stack.item is DyedWingBlockItem && color != -1) stack.getOrCreateTag()
                .putInt("Clockwork\$color", color)
            stack
        }
        return drops
    }

    override fun getCloneItemStack(level: BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        val stack = super.getCloneItemStack(level, pos, state)
        val be: ColorBlockEntity = (level.getBlockEntity(pos) as ColorBlockEntity)!!
        val color: Int = be.getColor()
        if (color != -1) {
            val tag = stack.getOrCreateTag()
            tag.putInt("Clockwork\$color", color)
        }
        return stack
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }
}