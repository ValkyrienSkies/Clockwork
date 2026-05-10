package org.valkyrienskies.clockwork.platform.forge

import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.content.processing.basin.BasinRecipe
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock
import com.simibubi.create.content.processing.recipe.ProcessingRecipe
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour
import com.simibubi.create.foundation.fluid.FluidIngredient
import com.simibubi.create.foundation.recipe.DummyCraftingContainer
import net.createmod.catnip.data.Iterate
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.items.IItemHandler
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import java.util.*
import java.util.function.Consumer
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min

object GasCrafterMethodsImpl {
    @JvmStatic
    fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
        val isGasCrafterRecipe = recipe is GasCraftingRecipe
        val basin = be.getBasin().getOrNull()
        val availableItems: IItemHandler? = basin?.getCapability(ForgeCapabilities.ITEM_HANDLER)?.orElse(null)
        val availableFluids: IFluidHandler? = basin?.getCapability(ForgeCapabilities.FLUID_HANDLER)?.orElse(null)

        val heat: BlazeBurnerBlock.HeatLevel = if (basin != null) BasinBlockEntity.getHeatLevelOf(basin.level!!.getBlockState(basin.blockPos.below())) else BlazeBurnerBlock.HeatLevel.NONE
        if (isGasCrafterRecipe && !recipe.getRequiredHeat()
                .testBlazeBurner(heat)
        ) return false

        val recipeOutputItems: MutableList<ItemStack?> = ArrayList<ItemStack?>()
        val recipeOutputFluids: MutableList<FluidStack?> = ArrayList<FluidStack?>()

        val ingredients: MutableList<Ingredient> = LinkedList<Ingredient>(recipe.getIngredients())
        val fluidIngredients =
            if (isGasCrafterRecipe) recipe.getFluidIngredients() else mutableListOf<FluidIngredient?>()

        val baseGasRecipe = (recipe as? GasCraftingRecipe)?.gasRecipe
        val gasIngredients = baseGasRecipe?.gasses?.toList() ?: emptyList()
        val gasResults = baseGasRecipe?.result?.toList() ?: emptyList()

        for (simulate in Iterate.trueAndFalse) {
            if (!simulate && test) return true


            val extractedFluidsFromTank = IntArray(availableFluids?.getTanks() ?: 0)
            val extractedItemsFromSlot = IntArray(availableItems?.getSlots() ?: 0)

            var itemsAffected = false
            if (availableItems != null)
            Ingredients@ for (ingredient in ingredients) {
                for (slot in 0..<availableItems.getSlots()) {
                    if (simulate && availableItems.getStackInSlot(slot)
                            .getCount() <= extractedItemsFromSlot[slot]
                    ) continue
                    val extracted = availableItems.extractItem(slot, 1, true)
                    if (!ingredient.test(extracted)) continue
                    if (!simulate) availableItems.extractItem(slot, 1, false)
                    extractedItemsFromSlot[slot]++
                    itemsAffected = true
                    continue@Ingredients
                }

                // something wasn't found
                return false
            }

            var fluidsAffected = false
            if (availableFluids != null)
            FluidIngredients@ for (fluidIngredient in fluidIngredients) {
                var amountRequired = fluidIngredient.getRequiredAmount().toInt()

                for (tank in 0..<availableFluids.getTanks()) {
                    val fluidStack = availableFluids.getFluidInTank(tank)
                    if (simulate && fluidStack.getAmount() <= extractedFluidsFromTank[tank]) continue
                    if (!fluidIngredient.test(fluidStack)) continue
                    val drainedAmount = min(amountRequired, fluidStack.getAmount())
                    if (!simulate) {
                        fluidStack.shrink(drainedAmount)
                    }
                    amountRequired -= drainedAmount
                    fluidsAffected = true
                    if (amountRequired != 0) continue
                    extractedFluidsFromTank[tank] += drainedAmount
                    continue@FluidIngredients
                }

                // something wasn't found
                return false
            }

            val currentMasses = ClockworkMod.getKelvin(be.level).getGasMassAt(be.getDuctNodePosition())
            GasIngredients@ for ((gasType, mass) in gasIngredients) {
                val mass = mass
                if ((currentMasses[gasType] ?: 0.0) < mass) return false
            }

            baseGasRecipe?.requirements?.forEach { (requirement, element) ->
                if (!requirement.apply_requirement(be.level!!, be.getDuctNodePosition(),
                        ClockworkMod.getKelvin(be.level), element)) return false
            }



            if (fluidsAffected) {
                basin!!.getBehaviour(SmartFluidTankBehaviour.INPUT)
                    .forEach(Consumer { obj: SmartFluidTankBehaviour.TankSegment? -> obj!!.onFluidStackChanged() })
                basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT)
                    .forEach(Consumer { obj: SmartFluidTankBehaviour.TankSegment? -> obj!!.onFluidStackChanged() })
            }

            // Note: this OR operation will break if input ingredients must have a count > 1
            // But right now, that can't happen, we hard code a required count of 1
            if (simulate && (itemsAffected || fluidsAffected)) {
                val remainderContainer: CraftingContainer =
                    DummyCraftingContainer(availableItems, extractedItemsFromSlot)

                if (recipe is GasCraftingRecipe) {
                    recipeOutputItems.addAll(recipe.rollResults())

                    for (fluidStack in recipe.getFluidResults()) if (!fluidStack.isEmpty()) recipeOutputFluids.add(
                        fluidStack
                    )
                    for (stack in recipe.getRemainingItems(remainderContainer)) if (!stack.isEmpty()) recipeOutputItems.add(
                        stack
                    )
                } else {
                    recipeOutputItems.add(recipe.getResultItem(basin!!.getLevel()!!.registryAccess()))

                    if (recipe is CraftingRecipe) {
                        for (stack in recipe.getRemainingItems(remainderContainer)) if (!stack.isEmpty()) recipeOutputItems.add(
                            stack
                        )
                    }
                }
            }
            if (basin != null && !basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate)) return false
            if (!simulate) {

                for ((gasType, mass) in gasResults) {
                    val mass = mass
                    ClockworkMod.getKelvin(be.level).modGasMass(be.getDuctNodePosition(), gasType, mass)
                }
            }
        }

        return true
    }
}