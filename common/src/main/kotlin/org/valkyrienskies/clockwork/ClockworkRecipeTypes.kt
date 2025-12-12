package org.valkyrienskies.clockwork

import com.simibubi.create.AllRecipeTypes.simpleType
import com.simibubi.create.Create
import com.simibubi.create.content.processing.recipe.ProcessingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo
import net.createmod.catnip.lang.Lang
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import java.util.function.Supplier

enum class ClockworkRecipeTypes(private val serializerSupplier: Supplier<RecipeSerializer<*>>,
                                private val typeSupplier: Supplier<RecipeType<*>>? = null,
                                private val registerType: Boolean = true
) : IRecipeTypeInfo {

    GAS_CRAFTING(ProcessingRecipeBuilder.ProcessingRecipeFactory(::GasCraftingRecipe));


    val id: ResourceLocation = Create.asResource(Lang.asId(name))

    // Constructor for ProcessingRecipeFactory (delegates to primary)
    constructor(processingFactory: ProcessingRecipeBuilder.ProcessingRecipeFactory<ProcessingRecipe<*>>): this(Supplier { ProcessingRecipeSerializer<ProcessingRecipe<*>>(processingFactory) })


    // Constructor for just a Serializer Supplier (delegates to primary with registerType = true)
    constructor(serializerSupplier: Supplier<RecipeSerializer<*>>) : this(
        serializerSupplier,
        null,
        true
    )



    // Init Logic for Serializer
    private val _serializerObject: RecipeSerializer<*> = Registry.register(
        BuiltInRegistries.RECIPE_SERIALIZER,
        id,
        serializerSupplier.get()
    )

    // Init Logic for Type
    private val _typeObject: RecipeType<*>? = if (registerType) {
        val t = typeSupplier?.get() ?: simpleType<Recipe<*>>(id)
        Registry.register(BuiltInRegistries.RECIPE_TYPE, id, t)
        t
    } else {
        null
    }

    private val _type: Supplier<RecipeType<*>> = if (registerType) {
        typeSupplier ?: Supplier { _typeObject!! }
    } else {
        typeSupplier!!
    }

    override fun getId(): ResourceLocation {
        return id
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : RecipeSerializer<*>> getSerializer(): T {
        return _serializerObject as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : RecipeType<*>> getType(): T {
        return _type.get() as T
    }


}