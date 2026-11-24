package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import org.valkyrienskies.clockwork.content.forces.BalloonPocketGasEffects
import org.valkyrienskies.kelvin.api.GasTag
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.abs

class GasBackTankItem(block: Block, properties: Properties) : BlockItem(block, properties), Equipable {

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)

        val tag = stack.tag ?: return
        syncBreathableGasFromAirTag(tag)
    }

    override fun getEquipmentSlot(): EquipmentSlot {
        return EquipmentSlot.CHEST
    }

    companion object {
        fun getWornBy(entity: Entity): GasBackTankItem? {
            return getWornStack(entity)?.item as? GasBackTankItem
        }

        fun getWornStack(entity: Entity): ItemStack? {
            if (entity !is LivingEntity) return null
            val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
            return chest.takeIf { it.item is GasBackTankItem }
        }

        fun readGasMasses(stack: ItemStack): Map<GasType, Double> {
            val tag = stack.tag ?: return emptyMap()
            return readGasMasses(tag)
        }

        fun readGasMasses(tag: CompoundTag): Map<GasType, Double> {
            val result = HashMap<GasType, Double>()
            for (key in tag.allKeys) {
                if (':' !in key) continue
                val loc = ResourceLocation.tryParse(key) ?: continue
                val gas = GasTypeRegistry.getGasType(loc) ?: continue
                val mass = tag.getDouble(key)
                if (mass.isFinite() && mass > 0.0) {
                    result[gas] = mass
                }
            }
            return result
        }

        fun readTemperature(stack: ItemStack): Double {
            val temp = stack.tag?.getDouble("KelvinTemperature") ?: 0.0
            return if (temp.isFinite() && temp > 0.0) temp else DEFAULT_TEMPERATURE
        }

        fun breathableMass(
            masses: Map<GasType, Double>,
            breathableTag: GasTag = BalloonPocketGasEffects.BREATHABLE_TAG
        ): Double {
            val tagLoaded = breathableTag.gases.isNotEmpty()
            var total = 0.0
            for ((gas, mass) in masses) {
                val isBreathable = if (tagLoaded) {
                    gas.hasTag(breathableTag)
                } else {
                    gas.resourceLocation == KELVIN_AIR
                }
                if (isBreathable) total += mass
            }
            return total
        }

        fun syncAirTagFromGases(
            tag: CompoundTag,
            breathableTag: GasTag = BalloonPocketGasEffects.BREATHABLE_TAG
        ) {
            val masses = readGasMasses(tag)
            val breathable = breathableMass(masses, breathableTag)
            tag.putFloat("Air", (breathable * AirKgsToAirTicks).toFloat())
        }

        /**
         * Keeps NBT gas masses in sync when Create's BacktankUtil modifies the `"Air"` float tag
         * (e.g., underwater diving or powering Create tools).
         */
        fun syncBreathableGasFromAirTag(
            tag: CompoundTag,
            breathableTag: GasTag = BalloonPocketGasEffects.BREATHABLE_TAG
        ) {
            val masses = readGasMasses(tag)
            val currentBreathable = breathableMass(masses, breathableTag)
            val airTagMass = (tag.getFloat("Air").toDouble() / AirKgsToAirTicks).coerceAtLeast(0.0)
            if (abs(airTagMass - currentBreathable) <= 0.01) return

            if (currentBreathable > 1e-9 && airTagMass < currentBreathable) {
                val scale = (airTagMass / currentBreathable).coerceIn(0.0, 1.0)
                val tagLoaded = breathableTag.gases.isNotEmpty()
                for ((gas, mass) in masses) {
                    val isBreathable = if (tagLoaded) gas.hasTag(breathableTag) else gas.resourceLocation == KELVIN_AIR
                    if (!isBreathable) continue
                    val updated = mass * scale
                    val key = gas.resourceLocation.toString()
                    if (updated <= 1e-6) tag.remove(key) else tag.putDouble(key, updated)
                }
            } else if (airTagMass > currentBreathable) {
                tag.putDouble(KELVIN_AIR.toString(), tag.getDouble(KELVIN_AIR.toString()) + (airTagMass - currentBreathable))
            }
        }

        /**
         * Proportionally consumes [amountKg] of gas from the backtank stack and updates `"Air"`.
         * Returns true if any gas was consumed.
         */
        fun consumeGas(
            stack: ItemStack,
            amountKg: Double,
            breathableTag: GasTag = BalloonPocketGasEffects.BREATHABLE_TAG
        ): Boolean {
            if (!amountKg.isFinite() || amountKg <= 0.0) return false
            val tag = stack.tag ?: return false
            val masses = readGasMasses(tag)
            val totalMass = masses.values.sum()
            if (!totalMass.isFinite() || totalMass <= 1e-9) return false

            val remainingFraction = ((totalMass - amountKg) / totalMass).coerceIn(0.0, 1.0)
            for ((gas, mass) in masses) {
                val updated = mass * remainingFraction
                val key = gas.resourceLocation.toString()
                if (updated <= 1e-6) {
                    tag.remove(key)
                } else {
                    tag.putDouble(key, updated)
                }
            }
            syncAirTagFromGases(tag, breathableTag)
            return true
        }

        private val KELVIN_AIR = ResourceLocation("kelvin", "air")
        private const val DEFAULT_TEMPERATURE = 288.15
        val AirKgsToAirTicks = 100.0
    }
}