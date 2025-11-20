package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class GasBackTankItem(block: Block, properties: Properties) : BlockItem(block, properties), Equipable {

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)

        if (stack.tag == null) return
        if (Math.abs(stack.tag!!.getFloat("Air")/AirKgsToAirTicks-stack.tag!!.getDouble("kelvin:air")) > 0.01) stack.tag!!.putDouble("kelvin:air", stack.tag!!.getFloat("Air")/AirKgsToAirTicks)


    }

    override fun getEquipmentSlot(): EquipmentSlot {
        return EquipmentSlot.CHEST
    }

    companion object {
        fun getWornBy(entity: Entity): GasBackTankItem? {

            if (entity !is LivingEntity) {
                return null
            }
            if (entity.getItemBySlot(EquipmentSlot.CHEST).item !is GasBackTankItem) {
                return null
            }
            return entity.getItemBySlot(EquipmentSlot.CHEST).item as GasBackTankItem
        }

        val AirKgsToAirTicks = 100.0
    }
}