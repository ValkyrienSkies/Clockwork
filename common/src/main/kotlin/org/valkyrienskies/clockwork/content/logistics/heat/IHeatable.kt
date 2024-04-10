package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasNodeResultData
import org.valkyrienskies.clockwork.kelvin.api.GasType
import java.util.EnumMap

interface IHeatable {

    var gasNodeID: GasNodeIdentifier?

    val gasMasses: EnumMap<GasType, Double>
    var temperature: Double
    var currentPressure: Double

    fun canTransferHeat(direction: Direction): Boolean

    fun getAttachedNeighbors(): EnumMap<Direction, IHeatable>
    fun getNeighborFlowRate(direction: Direction): Int
    fun getNeighborFlowDir(direction: Direction): MutableSet<Direction>

    fun isNeighborPipe(direction: Direction): Boolean

    fun getHeatLimit(): Double
    fun getPressureLimit(): Double

    fun recalculatePressure() {
        var pressure = 0.0
        for (gas in gasMasses.keys) {
            pressure += gasMasses[gas]!! * 8.31446261815324 * temperature / 0.375
        }
        currentPressure = pressure
    }

    fun applyUpdate(update: GasNodeResultData) {
        this.temperature = update.temperature
        this.gasMasses.clear()
        this.gasMasses.putAll(update.gasMasses)

        recalculatePressure()
    }
}