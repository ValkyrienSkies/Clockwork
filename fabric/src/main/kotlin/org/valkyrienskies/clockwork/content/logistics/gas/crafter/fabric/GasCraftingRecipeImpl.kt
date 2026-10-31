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
import java.util.ArrayList
import java.util.LinkedList
import kotlin.collections.iterator
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min

object GasCraftingRecipeImpl {

}