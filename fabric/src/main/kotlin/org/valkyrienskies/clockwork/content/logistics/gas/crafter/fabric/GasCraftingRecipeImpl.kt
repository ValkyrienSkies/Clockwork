package org.valkyrienskies.clockwork.content.logistics.gas.crafter.fabric

import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour
import com.simibubi.create.foundation.recipe.DummyCraftingContainer
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.minecraft.core.NonNullList
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.integration.jei.KelvinGasIngredient
import java.util.ArrayList
import java.util.LinkedList
import kotlin.collections.iterator
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min

object GasCraftingRecipeImpl {
    private fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
        val isGasCrafterRecipe = recipe is GasCraftingRecipe
        val basin = be.getBasin().getOrNull() ?: return false

        val availableItems = basin.getItemStorage(null)
        val availableFluids = basin.getFluidStorage(null)

        if (availableItems == null || availableFluids == null) return false

        val heat = BasinBlockEntity.getHeatLevelOf(basin.blockState)
        if (isGasCrafterRecipe && !recipe.requiredHeat.testBlazeBurner(heat)) return false

        val recipeOutputItems: MutableList<ItemStack> = ArrayList()
        val recipeOutputFluids: MutableList<FluidStack> = ArrayList()
        val recipeOutputGas: MutableList<KelvinGasIngredient> = ArrayList()

        val ingredients = LinkedList(recipe.ingredients)
        val fluidIngredients = if (isGasCrafterRecipe) recipe.fluidIngredients else emptyList()
        val gasIngredients = if (isGasCrafterRecipe) recipe.gasIngredients else emptyList()

        val consumedItems = NonNullList.create<ItemStack>()

        TransferUtil.getTransaction().use { t ->
            Ingredients@ for (ingredient in ingredients) {
                for (view in availableItems.nonEmptyViews()) {
                    val variant = view.resource
                    val stack = variant.toStack()
                    if (!ingredient.test(stack)) continue

                    // Catalyst items are never consumed
//                    val remainder = stack.recipeRemainder
//                    if (remainder.isEmpty && ItemStack.isSameItem(remainder, stack))
//                        continue@Ingredients

                    val extracted = view.extract(variant, 1, t)
                    if (extracted == 0L) continue
                    consumedItems.add(stack)
                    continue@Ingredients
                }
                // something wasn't found
                return false
            }

            var fluidsAffected = false
            FluidIngredients@ for (fluidIngredient in fluidIngredients) {
                var amountRequired = fluidIngredient.requiredAmount
                for (view: StorageView<FluidVariant> in availableFluids.nonEmptyViews()) {
                    val fluidStack = FluidStack(view)
                    if (!fluidIngredient.test(fluidStack)) continue
                    val drainedAmount = min(amountRequired, fluidStack.amount)
                    if (view.extract(fluidStack.type, drainedAmount, t) == drainedAmount) {
                        fluidsAffected = true
                        amountRequired -= drainedAmount
                        if (amountRequired != 0L) continue
                        continue@FluidIngredients
                    }
                }
                // something wasn't found
                return false
            }

            if (fluidsAffected) {
                TransactionCallback.onSuccess(t) {
                    basin.getBehaviour(SmartFluidTankBehaviour.INPUT)
                        .forEach { obj: SmartFluidTankBehaviour.TankSegment -> obj.onFluidStackChanged() }
                    basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT)
                        .forEach { obj: SmartFluidTankBehaviour.TankSegment -> obj.onFluidStackChanged() }
                }
            }

            val remainderContainer: CraftingContainer = DummyCraftingContainer(consumedItems)

            if (recipe is GasCraftingRecipe) {
                recipeOutputItems.addAll(recipe.rollResults())

                for (fluidStack in recipe.fluidResults)
                    if (!fluidStack.isEmpty)
                        recipeOutputFluids.add(fluidStack)

                for (stack in recipe.getRemainingItems(remainderContainer))
                    if (!stack.isEmpty)
                        recipeOutputItems.add(stack)

            }

            // fabric: bad
            recipeOutputItems.removeIf { it.isEmpty }

            if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, t)) return false

            val inputGasses: HashMap<GasType, Double> = hashMapOf()
            val currentMasses = ClockworkMod.getKelvin().getGasMassAt(be.getDuctNodePosition())
            GasIngredients@ for (gasIngredient in gasIngredients) {
                val mass = gasIngredient.moles * gasIngredient.gasType.density * 22.4
                inputGasses[gasIngredient.gasType] = mass

                if ((currentMasses[gasIngredient.gasType] ?: 0.0) < mass) return false
            }

            if (!test) {
                t.commit()

                for ((gas,deltaMass) in inputGasses) {
                    ClockworkMod.getKelvin().modGasMass(be.getDuctNodePosition(), gas, -deltaMass)
                }

                for (ingredient in gasIngredients) {
                    val mass = ingredient.moles * ingredient.gasType.density * 22.4
                    ClockworkMod.getKelvin().modGasMass(be.getDuctNodePosition(), ingredient.gasType, -mass)
                }

            }
            return true
        }
    }
}