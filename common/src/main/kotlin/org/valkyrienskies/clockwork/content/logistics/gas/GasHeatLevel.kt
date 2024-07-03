package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.foundation.utility.Lang
import net.minecraft.util.StringRepresentable

enum class GasHeatLevel: StringRepresentable {
    COOL,
    WARM,
    HOT,
    VERY_HOT,
    SUPER_HOT,
    MOLTEN;

    fun isAtLeast(gasHeatLevel: GasHeatLevel): Boolean {
        return ordinal >= gasHeatLevel.ordinal
    }

    override fun getSerializedName(): String {
        return Lang.asId(name)
    }

    companion object {
        fun byIndex(index: Int): GasHeatLevel {
            return values()[index]
        }
    }
}