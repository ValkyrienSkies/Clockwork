package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorItem.Type
import net.minecraft.world.item.Item.Properties
import org.valkyrienskies.clockwork.ClockworkArmorMaterials

class AeronautBootsItem(properties: Properties) : ArmorItem(
    ClockworkArmorMaterials.FLIGHT, Type.BOOTS,
    properties
), IAeronautEquipment {
}
