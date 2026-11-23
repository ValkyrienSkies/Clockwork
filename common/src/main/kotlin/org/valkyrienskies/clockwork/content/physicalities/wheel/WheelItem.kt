package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import org.valkyrienskies.clockwork.ClockworkRecipes
import org.valkyrienskies.clockwork.api.wheel.TireTypes
import org.valkyrienskies.clockwork.ClockworkItems

class WheelItem(properties: Properties, val size: WheelSize, val tireType: TireType?) : Item(properties) {
    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        if (tireType == TireType.WOOD) tooltip.add(Component.translatable("item.vs_clockwork.wood_wheel.material", wood(stack.tag).name))
    }
    override fun useOn(context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity ?: return InteractionResult.PASS
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild() || context.clickedFace != hub.facing) return InteractionResult.PASS
        val replacing = hub.wheel != null
        if (replacing) {
            if (!context.canSwap()) return InteractionResult.PASS
            val sameMaterial = when (tireType) {
                null -> hub.tireType != TireType.WOOD
                TireType.WOOD -> hub.tireType == tireType && hub.wood == wood(context.itemInHand.tag)
                else -> hub.tireType == tireType
            }
            if (hub.wheel == size && sameMaterial) return InteractionResult.FAIL
        }
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        if (!hub.install(size, player, context.itemInHand.tag, tireType, replacing)) return InteractionResult.FAIL
        if (!player.isCreative) context.itemInHand.shrink(1)
        return InteractionResult.CONSUME
    }

    companion object {
        fun wood(tag: CompoundTag?) = ResourceLocation.tryParse(tag?.getString("WheelWood") ?: "")
            ?.let { BuiltInRegistries.BLOCK.get(it) }?.takeIf { it.defaultBlockState().`is`(BlockTags.PLANKS) } ?: Blocks.OAK_PLANKS
    }
}

private fun UseOnContext.canSwap() = hand == InteractionHand.MAIN_HAND && player?.offhandItem?.item == ClockworkItems.SCREWDRIVER.get()

class TireItem(properties: Properties, val type: TireType) : Item(properties) {
    init { TireTypes.register(this) }

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        if (stack.hasTag()) {
            val condition = TireCondition()
            condition.read(stack.tag, type)
            tooltip.add(condition.status())
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity ?: return InteractionResult.PASS
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild() || context.clickedFace != hub.facing || hub.wheel == null || hub.tireType == TireType.WOOD)
            return InteractionResult.PASS
        if (hub.tireType != null && !context.canSwap()) return InteractionResult.FAIL
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        if (!hub.fitTire(type, player, context.canSwap(), context.itemInHand.tag)) return InteractionResult.FAIL
        if (!player.isCreative) context.itemInHand.shrink(1)
        return InteractionResult.CONSUME
    }
}

class WoodenWheelRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {
    private fun ingredients(container: CraftingContainer): Pair<ItemStack, BlockItem>? {
        val stacks = container.items.filterNot { it.isEmpty }
        if (stacks.size != 2) return null
        val wheel = stacks.singleOrNull { it.item is WheelItem && (it.item as WheelItem).tireType in listOf(null, TireType.WOOD) }
            ?: return null
        val planks = stacks.singleOrNull { (it.item as? BlockItem)?.block?.defaultBlockState()?.`is`(BlockTags.PLANKS) == true }
            ?.item as? BlockItem ?: return null
        return wheel to planks
    }

    override fun matches(container: CraftingContainer, level: Level) = ingredients(container) != null
    override fun canCraftInDimensions(width: Int, height: Int) = width * height >= 2
    override fun assemble(container: CraftingContainer, registryAccess: RegistryAccess): ItemStack {
        val (wheel, planks) = ingredients(container) ?: return ItemStack.EMPTY
        return ItemStack(WheelHubBlock.item((wheel.item as WheelItem).size, TireType.WOOD)).apply {
            tag = wheel.tag?.copy()
            orCreateTag.putString("WheelWood", BuiltInRegistries.BLOCK.getKey(planks.block).toString())
        }
    }
    override fun getSerializer() = ClockworkRecipes.WOODEN_WHEEL_SERIALIZER.get()
}
