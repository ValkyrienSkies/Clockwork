package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkItems

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class BladeData(val wide: Boolean, val angle: Double, val length: Double) {
    companion object {
        fun fromTag(nbt: CompoundTag): ArrayList<BladeData> {
            val tagBlades = nbt.getCompound("Blades") ?: return arrayListOf()
            val blades = arrayListOf<BladeData>()
            val tagBladesSize = nbt.getInt("BladeCount")

            val bladeLength = nbt.getDouble("BladeLength")
            val bladeAngle = nbt.getDouble("BladeAngle")

            for (i in 1..tagBladesSize) {
                val itemBlade = ItemStack.of(tagBlades.getCompound("Blade$i")) ?: continue
                blades.add(BladeData(itemBlade.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get()), bladeAngle, bladeLength))
            }
            return blades
        }
    }
}