package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity

interface IAeronautEquipment {
    companion object {
        fun LivingEntity.isWearingAeronautSet(): Boolean {
            return this.wearingAeronautInSlot(EquipmentSlot.HEAD) &&
                this.wearingAeronautInSlot(EquipmentSlot.CHEST) &&
                this.wearingAeronautInSlot(EquipmentSlot.LEGS) &&
                this.wearingAeronautInSlot(EquipmentSlot.FEET)
        }

        fun LivingEntity.isWearingAnyAeronaut(): Boolean {
            return this.wearingAeronautInSlot(EquipmentSlot.HEAD) ||
                this.wearingAeronautInSlot(EquipmentSlot.CHEST) ||
                this.wearingAeronautInSlot(EquipmentSlot.LEGS) ||
                this.wearingAeronautInSlot(EquipmentSlot.FEET)
        }

        fun LivingEntity.wearingAeronautInSlot(slot: EquipmentSlot): Boolean {
            return this.getItemBySlot(slot).item is IAeronautEquipment
        }
    }
}
