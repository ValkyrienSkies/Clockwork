package org.valkyrienskies.clockwork.util.blocktype

import com.simibubi.create.foundation.utility.Lang
import net.minecraft.util.StringRepresentable

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
    }
}