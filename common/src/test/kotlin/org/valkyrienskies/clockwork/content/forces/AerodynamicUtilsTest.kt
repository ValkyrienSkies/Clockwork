package org.valkyrienskies.clockwork.content.forces

import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.util.AerodynamicUtils

class AerodynamicUtilsTest {

    val dimensions = arrayOf("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end")
    val stringArray: Array<String> = arrayOf("Sea Level", "10 Blocks Higher", "Much Higher", "Nearing Limit", "Limit", "Above Limit")
    val altitudes: DoubleArray = doubleArrayOf(62.0, 73.0, 108.0, 562.0, 563.0, 564.0)

    @Test
    fun testGetAirDensityForY() {
        println("Density: ")
        for (dimension in dimensions) {
            println("In: $dimension")
            for (i in stringArray.indices) {
                println("${stringArray[i]}: ${AerodynamicUtils.getAirDensityForY(altitudes[i], dimension)}")
            }
        }

        println("----")
    }

    @Test
    fun testGetAirPressureForY() {
        println("Pressure: ")
        for (dimension in dimensions) {
            println("In: $dimension")
            for (i in stringArray.indices) {
                println("${stringArray[i]}: ${AerodynamicUtils.getAirPressureForY(altitudes[i], dimension)}")
            }
        }
        println("----")
    }

    @Test
    fun testGetAirTemperatureForY() {
        println("Temperature: ")
        for (dimension in dimensions) {
            println("In: $dimension")
            for (i in stringArray.indices) {
                println("${stringArray[i]}: ${AerodynamicUtils.getAirTemperatureForY(altitudes[i], dimension)}")
            }
        }
    }
}