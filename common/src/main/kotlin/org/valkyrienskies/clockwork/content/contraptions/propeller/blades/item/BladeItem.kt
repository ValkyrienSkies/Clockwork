package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import com.simibubi.create.content.equipment.armor.BacktankItem
import io.github.fabricators_of_create.porting_lib.tool.mixin.SwordItemMixin
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.level.Level

class BladeItem(tier: Tier, attackDamageModifier: Int, attackSpeedModifier: Float, properties: Properties) : SwordItem(tier, attackDamageModifier, attackSpeedModifier, properties) {


    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        // TODO: replace this with something better
        if (stack.orCreateTag.getDouble("BladeLength") == 0.0) stack.tag!!.putDouble("BladeLength",1.0)

        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    override fun getDefaultInstance(): ItemStack {
        val instance = super.getDefaultInstance()
        instance.orCreateTag.putDouble("BladeLength",1.0)
        //println(instance.tag?.getDouble("BladeLength"))
        return instance
    }



}
