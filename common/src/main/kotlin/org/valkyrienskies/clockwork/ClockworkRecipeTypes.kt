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
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import java.util.function.Supplier

enum class ClockworkRecipeTypes(private val serializerSupplier: Supplier<RecipeSerializer<*>>,
                                private val typeSupplier: Supplier<RecipeType<*>>? = null,
                                private val registerType: Boolean = true
) : IRecipeTypeInfo {

    GAS_CRAFTING(ProcessingRecipeBuilder.ProcessingRecipeFactory(::GasCraftingRecipe));

    private val SERIALIZER = DeferredRegister.create( ClockworkMod.MOD_ID,Registries.RECIPE_SERIALIZER)
    val registry = SERIALIZER.register(id, serializerSupplier)

    private val TYPE = DeferredRegister.create( ClockworkMod.MOD_ID,Registries.RECIPE_TYPE)

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
        TYPE.register(id) { t }
        println("$id was registered as recipe Type")
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
        return registry.get() as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : RecipeType<*>> getType(): T {
        return _type.get() as T
    }

    companion object {
        fun init() {
            // fabric: just load the class
        }

    }


}