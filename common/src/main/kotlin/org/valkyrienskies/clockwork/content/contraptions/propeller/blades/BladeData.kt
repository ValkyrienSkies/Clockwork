package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkItems

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class BladeData(val wide: Boolean, val angle: Double, val length: Double) {
    companion object {
        fun fromTag(nbt: CompoundTag): ArrayList<BladeData> {
            val tagBlades = nbt.getCompound("Blades") ?: return arrayListOf()
            val tagBladesSize = nbt.getInt("BladeCount")
            val newList: NonNullList<ItemStack> = NonNullList.withSize(8, ItemStack.EMPTY)
            ContainerHelper.loadAllItems(tagBlades, newList)
            val blades = arrayListOf<BladeData>()


            val bladeLength = nbt.getDouble("BladeLength")
            val bladeAngle = nbt.getDouble("BladeAngle")

            for (blade in newList) {
                if (blade.isEmpty) continue
                blades.add(BladeData(blade.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get()), bladeAngle, bladeLength))
            }
            return blades
        }
    }
}