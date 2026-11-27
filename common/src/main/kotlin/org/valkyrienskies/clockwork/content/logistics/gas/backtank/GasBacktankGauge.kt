package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

object GasBacktankGauge {
    const val NEEDLE_PIVOT_X_PX = 2.75f
    const val NEEDLE_PIVOT_Y_PX = 7f
    const val NEEDLE_PIVOT_Z_PX = 8f

    const val NEEDLE_PIVOT_X = NEEDLE_PIVOT_X_PX / 16f
    const val NEEDLE_PIVOT_Y = NEEDLE_PIVOT_Y_PX / 16f
    const val NEEDLE_PIVOT_Z = NEEDLE_PIVOT_Z_PX / 16f

    const val MAX_NEEDLE_DEGREES = 180f

    const val SHAKE_PRESSURE_THRESHOLD = 0.9
    const val MAX_SHAKE_DEGREES = 8f

    const val PRESSURE_SCALE_PA = 100_000.0
    private const val FALLBACK_TEMPERATURE_K = 293.15
    private const val AIR_GAS_ID = "kelvin:air"

    fun fillFromPressure(pressure: Double, maxPressure: Double = GasBacktankBlock.MAX_PRESSURE): Float {
        if (!pressure.isFinite() || pressure <= 0.0 || !maxPressure.isFinite() || maxPressure <= 0.0) {
            return 0f
        }
        val denom = ln(1.0 + maxPressure / PRESSURE_SCALE_PA)
        if (!denom.isFinite() || denom <= 0.0) return 0f
        return (ln(1.0 + pressure / PRESSURE_SCALE_PA) / denom).toFloat().coerceIn(0f, 1f)
    }

    fun shakeIntensity(pressure: Double, maxPressure: Double = GasBacktankBlock.MAX_PRESSURE): Float {
        if (!pressure.isFinite() || !maxPressure.isFinite() || maxPressure <= 0.0) return 0f
        val ratio = pressure / maxPressure
        if (ratio < SHAKE_PRESSURE_THRESHOLD) return 0f
        val over = ((ratio - SHAKE_PRESSURE_THRESHOLD) / (1.0 - SHAKE_PRESSURE_THRESHOLD)).coerceIn(0.0, 1.0)
        return (0.4 + 0.6 * over).toFloat()
    }

    fun needleShakeDegrees(
        pressure: Double,
        renderTime: Float,
        phase: Float = 0f,
        maxPressure: Double = GasBacktankBlock.MAX_PRESSURE
    ): Float {
        val intensity = shakeIntensity(pressure, maxPressure)
        if (intensity <= 0f || !renderTime.isFinite()) return 0f
        val wave = sin(renderTime * 2.3f + phase) * 0.65f + sin(renderTime * 5.7f + phase * 1.7f) * 0.35f
        return wave * MAX_SHAKE_DEGREES * intensity
    }

    fun needleAngleDegrees(fill: Float): Float {
        if (!fill.isFinite()) return 0f
        return fill.coerceIn(0f, 1f) * MAX_NEEDLE_DEGREES
    }

    fun needleAngleDegrees(
        fill: Float,
        pressure: Double,
        renderTime: Float,
        phase: Float = 0f,
        maxPressure: Double = GasBacktankBlock.MAX_PRESSURE
    ): Float {
        return needleAngleDegrees(fill) + needleShakeDegrees(pressure, renderTime, phase, maxPressure)
    }

    fun facingYRotationDegrees(facing: Direction): Float = when (facing) {
        Direction.EAST -> 0f
        Direction.SOUTH -> -90f
        Direction.WEST -> 180f
        Direction.NORTH -> 90f
        else -> 0f
    }

    fun pressureFromItemStack(stack: ItemStack): Double {
        val tag = stack.tag ?: return 0.0
        return pressureFromTag(tag)
    }

    fun fillFromItemStack(stack: ItemStack): Float {
        return fillFromPressure(pressureFromItemStack(stack))
    }

    fun pressureFromTag(tag: CompoundTag): Double {
        val rawTemp = tag.getDouble("KelvinTemperature")
        val temp = (if (rawTemp.isFinite() && rawTemp > 0.0) rawTemp else FALLBACK_TEMPERATURE_K)
            .coerceAtLeast(1e-4)
        val hasAirTicks = tag.contains("Air")
        val airMassFromTicks = if (hasAirTicks) {
            (tag.getFloat("Air").toDouble() / GasBackTankItem.AirKgsToAirTicks).coerceAtLeast(0.0)
        } else {
            0.0
        }

        var totalMass = 0.0
        var pressureCapacity = 0.0
        var sawKelvinAir = false

        for (key in tag.allKeys) {
            if (':' !in key) continue
            val rl = ResourceLocation.tryParse(key) ?: continue
            val gas = GasTypeRegistry.getGasType(rl) ?: continue
            var mass = tag.getDouble(key)
            if (key == AIR_GAS_ID) {
                sawKelvinAir = true
                if (hasAirTicks && abs(airMassFromTicks - mass) > 0.01) {
                    mass = airMassFromTicks
                }
            }
            if (!mass.isFinite() || mass <= 0.0) continue
            totalMass += mass
            val cv = (gas.specificHeatCapacity / gas.adiabaticIndex) * 1000.0
            pressureCapacity += mass * (gas.adiabaticIndex - 1.0) * cv
        }

        if (!sawKelvinAir && hasAirTicks && airMassFromTicks > 0.0) {
            val airGas = GasTypeRegistry.getGasType(ResourceLocation(AIR_GAS_ID))
            if (airGas != null) {
                totalMass += airMassFromTicks
                val cv = (airGas.specificHeatCapacity / airGas.adiabaticIndex) * 1000.0
                pressureCapacity += airMassFromTicks * (airGas.adiabaticIndex - 1.0) * cv
            }
        }

        if (totalMass <= 1e-12) return 0.0
        return (pressureCapacity / (GasBacktankBlock.VOLUME * GasBacktankBlock.TANK_SIZE)) * temp
    }
}
