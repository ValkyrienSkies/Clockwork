package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import org.valkyrienskies.clockwork.ClockworkArmorMaterials

class AeronautJacketItem(properties: Properties) : ArmorItem(
    ClockworkArmorMaterials.FLIGHT, Type.CHESTPLATE,
    properties
), IAeronautEquipment {
}
