package org.valkyrienskies.clockwork

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.BladeRecipeSerializer
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.CraftingTableBladeRecipe

object ClockworkRecipes {

    val RECIPE_SERIALIZERS: DeferredRegister<RecipeSerializer<*>?> = DeferredRegister.create(MOD_ID, Registries.RECIPE_SERIALIZER)
    val RECIPE_TYPES: DeferredRegister<RecipeType<*>> = DeferredRegister.create(MOD_ID, Registries.RECIPE_TYPE)

//    val BLADE_SERIALIZER: RegistrySupplier<RecipeSerializer<CraftingTableBladeRecipe>> = RECIPE_SERIALIZERS.register("blade_crafting", ::BladeRecipeSerializer)
//    val BLADE_RECIPE: RegistrySupplier<RecipeType<CraftingTableBladeRecipe>> = RECIPE_TYPES.register("blade_crafting") { CraftingTableBladeRecipe.BladeRecipeType }

    val BLADE_RECIPE_TYPE = RECIPE_TYPES.register("blade_crafting") {
        object : RecipeType<CraftingTableBladeRecipe> {
            override fun toString(): String {
                return "$MOD_ID:blade_crafting"
            }
        }
    }

    val BLADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("blade_crafting") {
        SimpleCraftingRecipeSerializer(::CraftingTableBladeRecipe)
    }

    fun init() {

        RECIPE_SERIALIZERS.register()
        RECIPE_TYPES.register()

        BLADE_RECIPE_TYPE.listen { println("Registered BLADE_RECIPE_TYPE") }
        BLADE_RECIPE_SERIALIZER.listen { println("Registered BLADE_RECIPE_SERIALIZER") }
    }
}