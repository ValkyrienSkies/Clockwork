package org.valkyrienskies.clockwork.util.fluid

import com.simibubi.create.foundation.fluid.SmartFluidTank
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.material.Fluid

interface CWFluidTank {
    val totalCapacity: Int
    val currentAmount: Int
    val spaceLeft: Int
        get() = Math.max(0, totalCapacity - currentAmount)
    val fluidType: Fluid?
    val isEmpty: Boolean
        get() = currentAmount <= 0

    fun store(tag: CompoundTag?): CompoundTag?
    fun read(tag: CompoundTag?)
    fun shrink(drainAmount: Int)
    fun grow(fillAmount: Int)
    fun asSmartFluidTank(): SmartFluidTank? {
        return this as SmartFluidTank
    }
}