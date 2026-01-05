package org.valkyrienskies.clockwork

import com.simibubi.create.content.processing.recipe.ProcessingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo
import dev.architectury.registry.registries.DeferredRegister
import net.createmod.catnip.lang.Lang
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.CraftingTableBladeRecipe
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.CuttingBladeRecipe
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import java.util.function.Supplier

object ClockworkRecipes {

    val RECIPE_SERIALIZERS: DeferredRegister<RecipeSerializer<*>?> = DeferredRegister.create(MOD_ID, Registries.RECIPE_SERIALIZER)
    val RECIPE_TYPES: DeferredRegister<RecipeType<*>> = DeferredRegister.create(MOD_ID, Registries.RECIPE_TYPE)

    val BLADE_CRAFTING_SERIALIZER = RECIPE_SERIALIZERS.register("blade_crafting") {
        SimpleCraftingRecipeSerializer(::CraftingTableBladeRecipe)
    }

    enum class ClockworkRecipeTypes(private val serializerSupplier: Supplier<RecipeSerializer<*>>,
                                    private val typeSupplier: Supplier<RecipeType<*>>? = null,
                                    private val registerType: Boolean = true
    ) : IRecipeTypeInfo {

        GAS_CRAFTING(ProcessingRecipeBuilder.ProcessingRecipeFactory(::GasCraftingRecipe)),
        BLADE_CUTTING(ProcessingRecipeBuilder.ProcessingRecipeFactory(::CuttingBladeRecipe));

        val serializer = RECIPE_SERIALIZERS.register(id, serializerSupplier)



        // Constructor for ProcessingRecipeFactory (delegates to primary)
        constructor(processingFactory: ProcessingRecipeBuilder.ProcessingRecipeFactory<ProcessingRecipe<*>>): this(Supplier { ProcessingRecipeSerializer<ProcessingRecipe<*>>(processingFactory) })


        // Constructor for just a Serializer Supplier (delegates to primary with registerType = true)
        constructor(serializerSupplier: Supplier<RecipeSerializer<*>>) : this(
            serializerSupplier,
            null,
            true
        )




        // Init Logic for Type
        private val _typeObject: RecipeType<*>? = if (registerType) {
            val t = typeSupplier?.get() ?: simpleType<Recipe<*>>(id)
            RECIPE_TYPES.register(id) { t }
            //println("$id was registered as recipe Type")
            t
        } else {
            null
        }

        fun <T : Recipe<*>?> simpleType(id: ResourceLocation): RecipeType<T?> {
            val stringId = id.toString()
            return object : RecipeType<T?> {
                override fun toString(): String {
                    return stringId
                }
            }
        }

        private val _type: Supplier<RecipeType<*>> = if (registerType) {
            typeSupplier ?: Supplier { _typeObject!! }
        } else {
            typeSupplier!!
        }

        override fun getId(): ResourceLocation {
            return ClockworkMod.asResource(Lang.asId(name))
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : RecipeSerializer<*>> getSerializer(): T {
            return serializer.get() as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : RecipeType<*>> getType(): T {
            return _type.get() as T
        }


        companion object {
            fun init() {}
        }
    }

    fun init() {

        RECIPE_SERIALIZERS.register()
        RECIPE_TYPES.register()
        ClockworkRecipeTypes.init()
    }
}
