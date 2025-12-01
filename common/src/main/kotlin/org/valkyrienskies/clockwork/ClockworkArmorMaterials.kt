package org.valkyrienskies.clockwork

import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.crafting.Ingredient
import java.util.function.Supplier

enum class ClockworkArmorMaterials(val resourceName: String, val maxDamageFactor: Int, val damageReductionAmountArray: IntArray, val enchantability: Int,
                                   val soundEvent: Supplier<SoundEvent>, val toughness: Float, val knockbackResistance: Float, val repairMaterial: Supplier<Ingredient>): ArmorMaterial {

    FLIGHT(ClockworkMod.asResource("flight").toString(), 33, intArrayOf(3, 6, 8, 3), 15,
        Supplier { ClockworkSounds.FLIGHTSUIT_EQUIP.mainEvent!! }, 2.0f, 0.1f,
        Supplier { Ingredient.of(ClockworkItems.WANDERLITE_CRYSTAL) }
    )
    ;

    override fun getDefenseForType(type: ArmorItem.Type): Int {
        return this.damageReductionAmountArray[type.ordinal]
    }

    override fun getDurabilityForType(type: ArmorItem.Type): Int {
        return MAX_DAMAGE_ARRAY[type.ordinal] * this.maxDamageFactor
    }
    override fun getEnchantmentValue(): Int {
        return this.enchantability
    }
    override fun getEquipSound(): SoundEvent {
        return this.soundEvent.get()
    }
    override fun getRepairIngredient(): Ingredient {
        return this.repairMaterial.get()
    }
    override fun getName(): String {
        return this.resourceName
    }

    override fun getKnockbackResistance(): Float {
        return this.knockbackResistance
    }
    override fun getToughness(): Float {
        return this.toughness
    }

    companion object {
        @JvmStatic
        private val MAX_DAMAGE_ARRAY: IntArray = intArrayOf(11, 16, 15, 13);
    }

}
