package org.valkyrienskies.clockwork.util.fluid.frosting

import net.minecraft.nbt.CompoundTag

data class FrostingAttributes(val burnTime: Int, val efficiency: Double, val power: Double, val stability: Double, val special: FrostingEffects) {
    /*
    Burn Time: 2mB/t
    Efficiency: 100%
    Power: 1
    Stability: 100%
    Special: None
     */
    companion object {
        fun getDefault(): FrostingAttributes =
                FrostingAttributes(2, 1.0, 1.0, 1.0, FrostingEffects.NONE)

        fun loadFromTag(tag: CompoundTag): FrostingAttributes = FrostingAttributes(
                tag.getInt("clockwork\$burnTime"),
                tag.getDouble("clockwork\$efficiency"),
                tag.getDouble("clockwork\$power"),
                tag.getDouble("clockwork\$stability"),
                FrostingAttributes.FrostingEffects.valueOf(tag.getString("clockwork\$special"))
        )
    }

    fun saveToTag(): CompoundTag {
        val tag = CompoundTag()

        tag.putInt("clockwork\$burnTime", this.burnTime)
        tag.putDouble("clockwork\$efficiency", this.efficiency)
        tag.putDouble("clockwork\$power", this.power)
        tag.putDouble("clockwork\$stability", this.stability)
        tag.putString("clockwork\$special", this.special.name)

        return tag
    }

    enum class FrostingEffects {
        NONE
    }
}
