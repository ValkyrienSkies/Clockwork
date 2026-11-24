package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkRecipes.ClockworkRecipeTypes.SIPHONING
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.abs

class SiphoningRecipe(private val recipeId: ResourceLocation, val entityType: EntityType<*>,
                      val gas: GasType, val massAt64Rpm: Double) : Recipe<SiphoningRecipe.Input> {
    class Input(val entityType: EntityType<*>) : SimpleContainer(0)

    fun massPerTick(rpm: Double) = massAt64Rpm * (abs(rpm) / 64.0) * ClockworkConfig.SERVER.siphonHarpoon.yieldMultiplier

    override fun matches(input: Input, level: Level) = input.entityType == entityType
    override fun assemble(input: Input, access: RegistryAccess): ItemStack = ItemStack.EMPTY
    override fun getResultItem(access: RegistryAccess): ItemStack = ItemStack.EMPTY
    override fun canCraftInDimensions(width: Int, height: Int) = false
    override fun isSpecial() = true
    override fun getId() = recipeId
    override fun getSerializer(): RecipeSerializer<*> = Serializer
    override fun getType(): RecipeType<SiphoningRecipe> = SIPHONING.getType()

    object Serializer : RecipeSerializer<SiphoningRecipe> {
        override fun fromJson(id: ResourceLocation, json: JsonObject): SiphoningRecipe = read(id,
            ResourceLocation(GsonHelper.getAsString(json, "entity")), ResourceLocation(GsonHelper.getAsString(json, "gas")),
            GsonHelper.getAsDouble(json, "kg_per_tick_at_64_rpm"))

        override fun fromNetwork(id: ResourceLocation, buf: FriendlyByteBuf) =
            read(id, buf.readResourceLocation(), buf.readResourceLocation(), buf.readDouble())

        private fun read(id: ResourceLocation, entity: ResourceLocation, gas: ResourceLocation, mass: Double): SiphoningRecipe {
            if (!mass.isFinite() || mass <= 0.0) throw JsonSyntaxException("Expected a positive finite gas yield in $id")
            val target = BuiltInRegistries.ENTITY_TYPE.getOptional(entity).orElseThrow { JsonSyntaxException("Unknown entity $entity in $id") }
            val output = GasTypeRegistry.getGasType(gas) ?: throw JsonSyntaxException("Unknown gas $gas in $id")
            return SiphoningRecipe(id, target, output, mass)
        }

        override fun toNetwork(buf: FriendlyByteBuf, recipe: SiphoningRecipe) {
            buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(recipe.entityType))
            buf.writeResourceLocation(recipe.gas.resourceLocation)
            buf.writeDouble(recipe.massAt64Rpm)
        }
    }
}
