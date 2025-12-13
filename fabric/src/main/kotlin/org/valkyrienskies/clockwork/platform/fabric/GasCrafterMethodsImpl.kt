package org.valkyrienskies.clockwork.platform.fabric

import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock
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
import java.util.ArrayList
import java.util.LinkedList
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.use

object GasCrafterMethodsImpl {
    @JvmStatic
    fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
        val isGasCrafterRecipe = recipe is GasCraftingRecipe
        val basin = be.getBasin().getOrNull()

        val availableItems = basin?.getItemStorage(null)
        val availableFluids = basin?.getFluidStorage(null)

        val heat = if (basin != null)  BasinBlockEntity.getHeatLevelOf(basin.blockState) else BlazeBurnerBlock.HeatLevel.NONE
        if (isGasCrafterRecipe && !recipe.requiredHeat.testBlazeBurner(heat)) return false

        val recipeOutputItems: MutableList<ItemStack> = ArrayList()
        val recipeOutputFluids: MutableList<FluidStack> = ArrayList()


        val ingredients = LinkedList(recipe.ingredients)
        val fluidIngredients = if (isGasCrafterRecipe) recipe.fluidIngredients else emptyList()

        val baseGasRecipe = (recipe as? GasCraftingRecipe)?.gasRecipe
        val gasIngredients = baseGasRecipe?.gasses?.toList() ?: emptyList()
        val gasResults = baseGasRecipe?.result?.toList() ?: emptyList()

        val consumedItems = NonNullList.create<ItemStack>()

        TransferUtil.getTransaction().use { t ->

            var itemsAffected = false
            if (availableItems != null)
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
                    itemsAffected = true
                    continue@Ingredients
                }
                // something wasn't found
                return false
            }

            var fluidsAffected = false
            if (availableFluids != null)
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
                    basin!!.getBehaviour(SmartFluidTankBehaviour.INPUT)
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

                baseGasRecipe?.requirements?.forEach { (requirement, element) ->
                    if (!requirement.apply_requirement(be.level!!, be.getDuctNodePosition(),
                        ClockworkMod.getKelvin(), element)) return false
                }
            }

            // fabric: bad
            recipeOutputItems.removeIf { it.isEmpty }

            if (itemsAffected && !basin!!.acceptOutputs(recipeOutputItems, recipeOutputFluids, t)) return false

            val currentMasses = ClockworkMod.getKelvin().getGasMassAt(be.getDuctNodePosition())
            GasIngredients@ for ((gasType, mass) in gasIngredients) {
                val mass = mass
                if ((currentMasses[gasType] ?: 0.0) < mass) return false
            }

            if (!test) {
                t.commit()

                for ((gas, deltaMass) in gasIngredients) {
                    ClockworkMod.getKelvin().modGasMass(be.getDuctNodePosition(), gas, -deltaMass)
                }

                for ((gasType, mass) in gasResults) {
                    val mass = mass
                    ClockworkMod.getKelvin().modGasMass(be.getDuctNodePosition(), gasType, mass)
                }

                ClockworkMod.getKelvin().modHeatEnergy(be.getDuctNodePosition(), baseGasRecipe?.energy ?: 0.0)

            }
            return true
        }
    }
}