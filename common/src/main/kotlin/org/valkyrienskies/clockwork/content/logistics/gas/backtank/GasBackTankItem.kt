package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.content.equipment.armor.BacktankItem
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Wearable
import net.minecraft.world.level.block.Block
import java.util.*

class GasBackTankItem(block: Block, properties: Properties) : BlockItem(block, properties), Wearable {


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
    }
}