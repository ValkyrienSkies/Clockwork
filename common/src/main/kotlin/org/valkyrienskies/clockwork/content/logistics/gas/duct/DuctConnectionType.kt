package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.foundation.utility.Lang
import net.minecraft.util.StringRepresentable

enum class DuctConnectionType(private val stringName: String): StringRepresentable {
    SIDE("true"), FORCED_OFF("forced"), NONE("false");

    val isConnected: Boolean
        get() = this == SIDE

    fun canBeChanged(): Boolean {
        return this == SIDE || this == NONE
    }

    override fun getSerializedName(): String {
        return Lang.asId(stringName)
    }
}