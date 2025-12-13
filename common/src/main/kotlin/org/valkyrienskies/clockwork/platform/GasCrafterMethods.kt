package org.valkyrienskies.clockwork.platform

import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour
import com.simibubi.create.foundation.recipe.DummyCraftingContainer
import dev.architectury.injectables.annotations.ExpectPlatform
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
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.use

object GasCrafterMethods {
    @JvmStatic
    @ExpectPlatform
    fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
        throw AssertionError()
    }
}