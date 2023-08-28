package org.valkyrienskies.clockwork.util.blocktype

import com.simibubi.create.foundation.utility.Lang
import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.material.Fluid
import org.valkyrienskies.clockwork.data.ClockWorkTags

enum class LiquidFuelType : StringRepresentable {
    NONE,
    STALE,
    PLAIN,
    SWEET,
    GOURMET,
    EXTRA;

    fun isAtLeast(fuelType: LiquidFuelType): Boolean {
        return ordinal >= fuelType.ordinal
    }

    override fun getSerializedName(): String {
        return Lang.asId(name)
    }

    companion object {
        fun byIndex(index: Int): LiquidFuelType {
            return values()[index]
        }

        fun fromFluid(fuel: Fluid?): LiquidFuelType {
            return if (ClockworkTags.AllFluidTags.STALE.matches(fuel)) {
                STALE
            } else if (ClockworkTags.AllFluidTags.PLAIN.matches(fuel)) {
                PLAIN
            } else if (ClockworkTags.AllFluidTags.SWEET.matches(fuel)) {
                SWEET
            } else if (ClockworkTags.AllFluidTags.GOURMET.matches(fuel)) {
                GOURMET
            } else if (ClockworkTags.AllFluidTags.EXTRA.matches(fuel)) {
                EXTRA
            } else {
                NONE
            }
        }
    }
}