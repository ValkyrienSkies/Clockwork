package org.valkyrienskies.clockwork.compat.jei

import com.simibubi.create.AllBlocks
import com.simibubi.create.compat.jei.CreateJEI.consumeTypedRecipes
import com.simibubi.create.compat.jei.DoubleItemIcon
import com.simibubi.create.compat.jei.EmptyBackground
import com.simibubi.create.compat.jei.ItemIcon
import com.simibubi.create.compat.jei.category.CreateRecipeCategory
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo
import com.simibubi.create.infrastructure.config.AllConfigs
import com.simibubi.create.infrastructure.config.CRecipes
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.builder.IRecipeSlotBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.runtime.IIngredientManager
import mezz.jei.api.runtime.IJeiRuntime
import net.createmod.catnip.config.ConfigBase
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.ItemLike
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkRecipes
import org.valkyrienskies.clockwork.compat.jei.categories.GasCrafterCategory
import org.valkyrienskies.clockwork.compat.jei.categories.GasReactionCategory
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient
import org.valkyrienskies.kelvin.integration.jei.GasIngredientRenderer
import org.valkyrienskies.kelvin.integration.jei.GasIngredientType
import org.valkyrienskies.kelvin.integration.jei.KelvinJeiPlugin
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import javax.annotation.Nonnull

@JeiPlugin
@Suppress("unused", "unchecked_cast")
class ClockworkJEI() : IModPlugin {
    private var ingredientManager: IIngredientManager? = null
    private val ID: ResourceLocation = ClockworkMod.asResource("jei_plugin")
    var runtime: IJeiRuntime? = null


    private fun loadCategories() {
        allCategories.clear()

        builder(GasCraftingRecipe::class.java)
            .addTypedRecipes(ClockworkRecipes.ClockworkRecipeTypes.GAS_CRAFTING)
            .catalyst { ClockworkBlocks.GAS_CRAFTER.get() }
            .catalyst { AllBlocks.BASIN.get() }
            .itemIcon(ClockworkBlocks.GAS_CRAFTER.get())
            .build(
                "gas_crafting",
                CreateRecipeCategory.Factory { info: CreateRecipeCategory.Info<GasCraftingRecipe> ->
                    GasCrafterCategory(info)
                })
    }
        

    private fun <T : Recipe<*>?> builder(recipeClass: Class<out T?>):CategoryBuilder<T?> {
        return CategoryBuilder<T?>(recipeClass)
    }

    @Nonnull
    override fun getPluginUid(): ResourceLocation {
        return ID
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        loadCategories()
        registration.addRecipeCategories(*allCategories.toTypedArray())

        registration.addRecipeCategories(GasReactionCategory())
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        allCategories.forEach(Consumer { c: CreateRecipeCategory<*>? -> c!!.registerCatalysts(registration) })

        registration.addRecipeCatalyst(ClockworkBlocks.DUCT.asStack(), KelvinJeiPlugin.GAS_REACTION_RECIPE_TYPE)
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        allCategories.forEach(Consumer { c: CreateRecipeCategory<*>? -> c!!.registerRecipes(registration) })

    }


    class CategoryBuilder<T : Recipe<*>?>(private val recipeClass: Class<out T?>) {
        private var predicate = Predicate { cRecipes: CRecipes? -> true }

        private var background: IDrawable? = null
        private var icon: IDrawable? = null

        private val recipeListConsumers: MutableList<Consumer<MutableList<T?>?>> =
            ArrayList<Consumer<MutableList<T?>?>>()
        private val catalysts: MutableList<Supplier<out ItemStack?>?> = ArrayList<Supplier<out ItemStack?>?>()

        fun enableIf(predicate: Predicate<CRecipes?>): CategoryBuilder<T?> {
            this.predicate = predicate
            return this as CategoryBuilder<T?>
        }

        fun enableWhen(configValue: Function<CRecipes?, ConfigBase.ConfigBool?>): CategoryBuilder<T?> {
            predicate = Predicate { c: CRecipes? -> configValue.apply(c)!!.get() }
            return this as CategoryBuilder<T?>
        }

        fun addRecipeListConsumer(consumer: Consumer<MutableList<T?>?>): CategoryBuilder<T?> {
            recipeListConsumers.add(consumer)
            return this as CategoryBuilder<T?>
        }

        fun addRecipes(collection: Supplier<MutableCollection<out T?>?>): CategoryBuilder<T?> {
            return addRecipeListConsumer(Consumer { recipes: MutableList<T?>? -> recipes!!.addAll(collection.get()!!) })
        }

        fun addAllRecipesIf(pred: Predicate<Recipe<*>?>): CategoryBuilder<T?> {
            return addRecipeListConsumer(Consumer { recipes: MutableList<T?>? ->
                consumeAllRecipes(
                    Consumer { recipe: Recipe<*>? ->
                        if (pred.test(recipe)) recipes!!.add(recipe as T?)
                    })
            })
        }

        fun addAllRecipesIf(
            pred: Predicate<Recipe<*>?>,
            converter: Function<Recipe<*>?, T?>
        ): CategoryBuilder<T?> {
            return addRecipeListConsumer(Consumer { recipes: MutableList<T?>? ->
                consumeAllRecipes(
                    Consumer { recipe: Recipe<*>? ->
                        if (pred.test(recipe)) {
                            recipes!!.add(converter.apply(recipe))
                        }
                    })
            })
        }

        fun addTypedRecipes(recipeTypeEntry: IRecipeTypeInfo): CategoryBuilder<T?> {
            return addTypedRecipes(Supplier { recipeTypeEntry.getType() })
        }

        fun addTypedRecipes(recipeType: Supplier<net.minecraft.world.item.crafting.RecipeType<out T?>?>): CategoryBuilder<T?> {
            return addRecipeListConsumer({ recipes: MutableList<T?>? ->
                consumeTypedRecipes(
                     { e: T? -> recipes!!.add(e) },
                    recipeType.get()
                )
            })
        }

        fun addTypedRecipes(
            recipeType: Supplier<net.minecraft.world.item.crafting.RecipeType<out T?>?>,
            converter: Function<Recipe<*>?, T?>
        ): CategoryBuilder<T?> {
            return addRecipeListConsumer(Consumer { recipes: MutableList<T?>? ->
                consumeTypedRecipes(
                     { recipe: T? -> recipes!!.add(converter.apply(recipe)) },
                    recipeType.get()
                )
            })
        }



        fun catalystStack(supplier: Supplier<ItemStack?>): CategoryBuilder<T?> {
            catalysts.add(supplier)
            return this as CategoryBuilder<T?>
        }

        fun catalyst(supplier: Supplier<ItemLike?>): CategoryBuilder<T?> {
            return catalystStack(Supplier {
                ItemStack(
                    supplier.get()!!
                        .asItem()
                )
            })
        }

        fun icon(icon: IDrawable): CategoryBuilder<T?> {
            this.icon = icon
            return this as CategoryBuilder<T?>
        }

        fun itemIcon(item: ItemLike): CategoryBuilder<T?> {
            icon(ItemIcon(Supplier { ItemStack(item) }))
            return this as CategoryBuilder<T?>
        }

        fun doubleItemIcon(item1: ItemLike, item2: ItemLike): CategoryBuilder<T?> {
            icon(DoubleItemIcon({ ItemStack(item1) }, { ItemStack(item2) }))
            return this as CategoryBuilder<T?>
        }

        fun background(background: IDrawable): CategoryBuilder<T?> {
            this.background = background
            return this as CategoryBuilder<T?>
        }

        fun emptyBackground(width: Int, height: Int): CategoryBuilder<T?> {
            background(EmptyBackground(width, height))
            return this as CategoryBuilder<T?>
        }

        fun build(name: String, factory: CreateRecipeCategory.Factory<T?>): CreateRecipeCategory<T?> {
            val recipesSupplier: Supplier<MutableList<T?>?>?
            if (predicate.test(AllConfigs.server().recipes)) {
                recipesSupplier = Supplier {
                    val recipes: MutableList<T?> = ArrayList<T?>()
                    for (consumer in recipeListConsumers) consumer.accept(recipes)
                    recipes
                }
            } else {
                recipesSupplier = Supplier { mutableListOf<T?>() }
            }

            val info = CreateRecipeCategory.Info<T?>(
                RecipeType<T?>(ClockworkMod.asResource(name), recipeClass),
                ClockworkLang.translateDirect("recipe." + name), background, icon, recipesSupplier, catalysts
            )
            val category = factory.create(info)
            allCategories.add(category)
            return category
        }
    }

    override fun onRuntimeAvailable(runtime: IJeiRuntime) {
        this.runtime = runtime
    }


    companion object {
        fun consumeAllRecipes(consumer: Consumer<Recipe<*>?>) {
            Minecraft.getInstance()
                .getConnection()!!
                .getRecipeManager()
                .getRecipes()
                .forEach(consumer)
        }

        fun addInputGasSlot(builder: IRecipeLayoutBuilder, x: Int, y: Int, ingredient: KelvinGasIngredient, background: IDrawable = BASIC_SLOT): IRecipeSlotBuilder {
            return addGasSlot(builder, x, y, RecipeIngredientRole.INPUT, background)
                .addIngredient(KelvinJeiPlugin.GAS_INGREDIENT_TYPE, ingredient)
        }

        fun addOutputGasSlot(builder: IRecipeLayoutBuilder, x: Int, y: Int, ingredient: KelvinGasIngredient, background: IDrawable = BASIC_SLOT): IRecipeSlotBuilder {
            return addGasSlot(builder, x, y, RecipeIngredientRole.OUTPUT, background)
                .addIngredient(KelvinJeiPlugin.GAS_INGREDIENT_TYPE, ingredient)
        }

        fun addGasSlot(builder: IRecipeLayoutBuilder, x: Int, y: Int, role: RecipeIngredientRole, background: IDrawable): IRecipeSlotBuilder {
            return builder.addSlot(role, x, y)
                .setBackground(background, -1, -1)
                .setCustomRenderer(GasIngredientType(), GasIngredientRenderer())
        }

        fun asDrawable(texture: AllGuiTextures): IDrawable {
            return object : IDrawable {
                override fun getWidth(): Int {
                    return texture.getWidth()
                }

                override fun getHeight(): Int {
                    return texture.getHeight()
                }

                override fun draw(graphics: GuiGraphics, xOffset: Int, yOffset: Int) {
                    texture.render(graphics, xOffset, yOffset)
                }
            }
        }

        val BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);

        private val allCategories: MutableList<CreateRecipeCategory<*>?> = ArrayList<CreateRecipeCategory<*>?>()
    }

}