package org.valkyrienskies.clockwork.content.logistics.gas.engine

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

object GasEngineLogic {
    const val EFFICIENCY_STEPS = 6
    const val BAR_SEGMENTS = 18
    const val DEFAULT_TEMPERATURE_INCREMENT = 290.0
    const val TEMPERATURE_OFFSET = 80.0

    fun calculateEfficiency(
        level: Level,
        blockPos: BlockPos,
        ductNodePos: DuctNodePos,
        axis: Direction.Axis,
        flowForFullEfficiency: Double,
        minimumFlowRate: Double,
        flowRateIncrement: Double,
        temperatureIncrement: Double
    ): Float {
        return calculateEfficiencyComponents(
            level,
            blockPos,
            ductNodePos,
            axis,
            flowForFullEfficiency,
            minimumFlowRate,
            flowRateIncrement,
            temperatureIncrement
        ).totalEfficiency
    }

    fun calculateEfficiencyComponents(
        level: Level,
        blockPos: BlockPos,
        ductNodePos: DuctNodePos,
        axis: Direction.Axis,
        flowForFullEfficiency: Double,
        minimumFlowRate: Double,
        flowRateIncrement: Double,
        temperatureIncrement: Double
    ): EfficiencyComponents {
        val kelvin = ClockworkMod.getKelvin(level)
        val temperatureEfficiency = calculateTemperatureEfficiency(kelvin, ductNodePos, temperatureIncrement)
        val flowRate = getEffectiveThroughput(kelvin, level, blockPos, ductNodePos, axis, minimumFlowRate, flowRateIncrement)
        val flowEfficiency = flowToEfficiency(flowRate, flowForFullEfficiency)

        return EfficiencyComponents(temperatureEfficiency, flowEfficiency, flowRate)
    }

    fun calculateTemperatureEfficiency(level: Level, ductNodePos: DuctNodePos, temperatureIncrement: Double): Float {
        return calculateTemperatureEfficiency(ClockworkMod.getKelvin(level), ductNodePos, temperatureIncrement)
    }

    fun calculateTemperatureEfficiency(
        kelvin: DuctNetwork<*>,
        ductNodePos: DuctNodePos,
        temperatureIncrement: Double
    ): Float {
        return tempToEfficiency(kelvin.getTemperatureAt(ductNodePos), temperatureIncrement).coerceIn(0f, 1f)
    }

    fun tempToEfficiency(temperature: Double): Float {
        return tempToEfficiency(temperature, DEFAULT_TEMPERATURE_INCREMENT)
    }

    fun tempToEfficiency(temperature: Double, temperatureIncrement: Double): Float {
        val safeIncrement = if (temperatureIncrement > 0.0) temperatureIncrement else DEFAULT_TEMPERATURE_INCREMENT
        val steps = floor((temperature - TEMPERATURE_OFFSET) / safeIncrement).toInt()
        return (steps / EFFICIENCY_STEPS.toFloat()).coerceAtLeast(0f)
    }

    fun smoothEfficiency(current: Float, target: Float, smoothing: Double): Float {
        if (smoothing <= 0.0) return target

        val clampedSmoothing = smoothing.coerceAtMost(1.0).toFloat()
        val next = current + (target - current) * clampedSmoothing
        return if (abs((target - next).toDouble()) < 0.0001) target else next
    }

    fun getEffectiveThroughput(
        level: Level,
        blockPos: BlockPos,
        ductNodePos: DuctNodePos,
        axis: Direction.Axis,
        minimumFlowRate: Double,
        flowRateIncrement: Double
    ): Double {
        return getEffectiveThroughput(
            ClockworkMod.getKelvin(level),
            level,
            blockPos,
            ductNodePos,
            axis,
            minimumFlowRate,
            flowRateIncrement
        )
    }

    fun getEffectiveThroughput(
        kelvin: DuctNetwork<*>,
        level: Level,
        blockPos: BlockPos,
        ductNodePos: DuctNodePos,
        axis: Direction.Axis,
        minimumFlowRate: Double,
        flowRateIncrement: Double
    ): Double {
        val throughput = getThroughput(kelvin, level, blockPos, ductNodePos, axis)
        if (throughput < minimumFlowRate) return 0.0
        return stepValue(throughput, flowRateIncrement)
    }

    fun getThroughput(level: Level, blockPos: BlockPos, ductNodePos: DuctNodePos, axis: Direction.Axis): Double {
        return getThroughput(ClockworkMod.getKelvin(level), level, blockPos, ductNodePos, axis)
    }

    fun getThroughput(
        kelvin: DuctNetwork<*>,
        level: Level,
        blockPos: BlockPos,
        ductNodePos: DuctNodePos,
        axis: Direction.Axis
    ): Double {
        var totalIn = 0.0
        var totalOut = 0.0

        for (axisDirection in Direction.AxisDirection.entries) {
            val direction = Direction.fromAxisAndDirection(axis, axisDirection)
            val neighborPos = blockPos.relative(direction)
            val neighborNodePos = ClockworkUtils.getDuctNodePos(neighborPos, level)
            val edge = kelvin.getEdgeBetween(ductNodePos, neighborNodePos) ?: continue
            val signedFlowOut = signedFlowOutOfNode(edge, ductNodePos)

            if (signedFlowOut > 0.0) totalOut += signedFlowOut
            if (signedFlowOut < 0.0) totalIn += -signedFlowOut
        }

        return min(totalIn, totalOut)
    }

    fun flowToEfficiency(throughput: Double, flowForFullEfficiency: Double): Float {
        if (flowForFullEfficiency <= 0.0) return if (throughput > 0.0) 1f else 0f
        return (throughput / flowForFullEfficiency).coerceIn(0.0, 1.0).toFloat()
    }

    fun getSpeedModifier(efficiency: Float): Int {
        return 1 + if (efficiency >= 1f) 3 else min(2.0, floor(efficiency * 4.0)).toInt()
    }

    fun roundEfficiencyForWholeStress(efficiency: Float, baseCapacity: Double): Float {
        if (efficiency <= 0f || baseCapacity <= 0.0) return 0f

        val generatedStressAtBaseSpeed = baseCapacity * 16.0
        if (generatedStressAtBaseSpeed <= 0.0) return 0f

        return (floor(efficiency * generatedStressAtBaseSpeed) / generatedStressAtBaseSpeed)
            .toFloat()
            .coerceIn(0f, efficiency)
    }

    private fun signedFlowOutOfNode(edge: DuctEdge, ductNodePos: DuctNodePos): Double {
        return when (ductNodePos) {
            edge.nodeA -> edge.currentFlowRate
            edge.nodeB -> -edge.currentFlowRate
            else -> 0.0
        }
    }

    private fun stepValue(value: Double, increment: Double): Double {
        if (increment <= 0.0) return value
        return floor(value / increment) * increment
    }

    data class EfficiencyComponents(
        val temperatureEfficiency: Float,
        val flowEfficiency: Float,
        val flowRate: Double
    ) {
        val totalEfficiency: Float get() = temperatureEfficiency * flowEfficiency
    }
}
