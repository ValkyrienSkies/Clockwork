package org.valkyrienskies.clockwork.content.logistics.gas.engine

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNodePos
import kotlin.math.floor
import kotlin.math.min

object GasEngineLogic {
    const val BAR_SEGMENTS = 20
    const val EFFICIENCY_STEPS = BAR_SEGMENTS
    const val DEFAULT_TEMPERATURE_INCREMENT = 60.0
    const val TEMPERATURE_OFFSET = 290.0

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
        val rawFlowRate = getThroughput(kelvin, level, blockPos, ductNodePos, axis)
        val flowRate = getEffectiveThroughput(rawFlowRate, minimumFlowRate, flowRateIncrement)
        val flowEfficiency = flowToEfficiency(flowRate, flowForFullEfficiency)

        return EfficiencyComponents(temperatureEfficiency, flowEfficiency, flowRate, rawFlowRate)
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

    fun temperatureUntilNextTier(temperature: Double, temperatureIncrement: Double): Double {
        val safeIncrement = if (temperatureIncrement > 0.0) temperatureIncrement else DEFAULT_TEMPERATURE_INCREMENT
        val tier = floor((temperature - TEMPERATURE_OFFSET) / safeIncrement).toInt().coerceIn(0, EFFICIENCY_STEPS)
        if (tier >= EFFICIENCY_STEPS) return 0.0

        val nextTierTemperature = TEMPERATURE_OFFSET + (tier + 1) * safeIncrement
        return (nextTierTemperature - temperature).coerceAtLeast(0.0)
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
        return getEffectiveThroughput(throughput, minimumFlowRate, flowRateIncrement)
    }

    fun getEffectiveThroughput(
        throughput: Double,
        minimumFlowRate: Double,
        flowRateIncrement: Double
    ): Double {
        if (throughput < minimumFlowRate) return 0.0
        return stepValue(throughput, flowRateIncrement)
    }

    fun flowUntilNextTierGramsPerTick(
        throughput: Double,
        flowForFullEfficiency: Double,
        minimumFlowRate: Double,
        flowRateIncrement: Double
    ): Double {
        return flowUntilNextTierKilogramsPerTick(
            throughput,
            flowForFullEfficiency,
            minimumFlowRate,
            flowRateIncrement
        ) * 1000.0
    }

    fun flowUntilNextTierKilogramsPerTick(
        throughput: Double,
        flowForFullEfficiency: Double,
        minimumFlowRate: Double,
        flowRateIncrement: Double
    ): Double {
        return flowUntilNextTier(throughput, flowForFullEfficiency, minimumFlowRate, flowRateIncrement) / 20.0
    }

    fun flowUntilNextTier(
        throughput: Double,
        flowForFullEfficiency: Double,
        minimumFlowRate: Double,
        flowRateIncrement: Double
    ): Double {
        if (flowForFullEfficiency <= 0.0) return 0.0

        val rawThroughput = throughput.coerceAtLeast(0.0)
        val minimum = minimumFlowRate.coerceAtLeast(0.0)
        if (rawThroughput < minimum) return minimum - rawThroughput

        val tierSize = if (flowRateIncrement > 0.0) flowRateIncrement else flowForFullEfficiency / EFFICIENCY_STEPS
        if (tierSize <= 0.0) return 0.0

        val fullTier = if (flowRateIncrement > 0.0)
            floor((flowForFullEfficiency + tierSize - 1e-9) / tierSize) * tierSize
        else flowForFullEfficiency
        if (rawThroughput >= fullTier) return 0.0

        val currentTier = floor(rawThroughput / tierSize) * tierSize
        val nextTier = (currentTier + tierSize).coerceAtLeast(minimum).coerceAtMost(fullTier)
        return (nextTier - rawThroughput).coerceAtLeast(0.0)
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
        val flowRate: Double,
        val rawFlowRate: Double
    ) {
        val totalEfficiency: Float get() = temperatureEfficiency * flowEfficiency
    }
}
