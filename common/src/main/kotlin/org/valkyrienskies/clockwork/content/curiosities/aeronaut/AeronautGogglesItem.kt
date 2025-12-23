package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.simibubi.create.content.equipment.goggles.GogglesItem
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorItem.Type.HELMET
import org.valkyrienskies.clockwork.ClockworkArmorMaterials

class AeronautGogglesItem(properties: Properties) : ArmorItem(ClockworkArmorMaterials.FLIGHT, HELMET, properties), IAeronautEquipment {
}
