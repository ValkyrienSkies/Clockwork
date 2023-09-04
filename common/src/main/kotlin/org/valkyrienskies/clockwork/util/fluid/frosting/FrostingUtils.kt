package org.valkyrienskies.clockwork.util.fluid.frosting

import com.simibubi.create.foundation.fluid.SmartFluidTank
import dev.architectury.fluid.FluidStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.material.Fluid
import org.valkyrienskies.clockwork.ClockworkFluids
import org.valkyrienskies.clockwork.util.fluid.CWFluidTank

object FrostingUtils {
    /*
    Burn Time: 2mB/t
    Efficiency: 100%
    Power: 1
    Stability: 100%
    Special: None
     */

    /**
     * Compares the input Fluid to the Registry
     */
    fun isFrosting(fluid: Fluid): Boolean =
            fluid == ClockworkFluids.FROSTING_FLUID.get() ||
                    fluid == ClockworkFluids.FROSTING_FLUID_FLOWING.get()

    /**
     * Compares the input LiquidBlock's fluid to the Registry
     */
    fun isFrosting(block: LiquidBlock): Boolean = isFrosting(block.`arch$getFluid`())

    /**
     * Compares the input Architectury FluidStack's fluid to the Registry
     */
    fun isFrosting(stack: FluidStack): Boolean = isFrosting(stack.fluid)

    /**
     * Compares the input PortingLib FluidStack's fluid to the Registry
     */
    fun isFrosting(stack: io.github.fabricators_of_create.porting_lib.util.FluidStack): Boolean = isFrosting(stack.fluid)

    /**
     * Compares the input CWFluidTank's fluid to the Registry
     */
    fun isFrosting(tank: CWFluidTank): Boolean {
        return isFrosting(tank.fluidType ?: return false)
    }

    /**
     * Compares the input SmartFluidTank's fluid to the Registry
     */
    fun isFrosting(tank: SmartFluidTank): Boolean = isFrosting(tank.fluid)

    /**
     * Get Attributes from an Architectury FluidStack
     */
    fun getAttributesFromStack(stack: FluidStack): FrostingAttributes {
        val tag = stack.tag?.get("clockwork\$attributes")
                ?: return FrostingAttributes.getDefault()
        return FrostingAttributes.loadFromTag(tag as CompoundTag)
    }

    /**
     * Set Attributes to an Architectury FluidStack
     */
    fun setAttributesToStack(stack: FluidStack, attributes: FrostingAttributes) =
            stack.orCreateTag.put("clockwork\$attributes", attributes.saveToTag())

    /**
     * Get Attributes from a PortingLib FluidStack
     */
    fun getAttributesFromStack(stack: io.github.fabricators_of_create.porting_lib.util.FluidStack): FrostingAttributes {
        val tag = stack.tag?.get("clockwork\$attributes")
                ?: return FrostingAttributes.getDefault()
        return FrostingAttributes.loadFromTag(tag as CompoundTag)
    }

    /**
     * Set Attributes to a PortingLib FluidStack
     */
    fun setAttributesToStack(stack: io.github.fabricators_of_create.porting_lib.util.FluidStack, attributes: FrostingAttributes) =
            stack.orCreateTag.put("clockwork\$attributes", attributes.saveToTag())
}