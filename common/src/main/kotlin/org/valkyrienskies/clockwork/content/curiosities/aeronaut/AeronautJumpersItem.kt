package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorItem.Type
import net.minecraft.world.item.Item.Properties
import org.valkyrienskies.clockwork.ClockworkArmorMaterials

class AeronautJumpersItem(properties: Properties) : ArmorItem(
    ClockworkArmorMaterials.FLIGHT, Type.LEGGINGS,
    properties
), IAeronautEquipment {
}
