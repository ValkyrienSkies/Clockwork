package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.foundation.utility.Lang
import net.minecraft.util.StringRepresentable

enum class DuctConnectionType(private val stringName: String): StringRepresentable {
    SIDE("true"), FORCED_OFF("forced"), TEMP_ON("temp"), NONE("false");

    val isConnected: Boolean
        get() = this == SIDE || this == TEMP_ON

    fun canBeChanged(): Boolean {
        return this.isConnected || this == NONE
    }

    override fun getSerializedName(): String {
        return Lang.asId(stringName)
    }
}