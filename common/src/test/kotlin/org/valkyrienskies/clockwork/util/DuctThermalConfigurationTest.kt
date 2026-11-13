package org.valkyrienskies.clockwork.util

import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.filter.edges.ClockworkFilteredOnewayDuctEdge
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos

class DuctThermalConfigurationTest {
    @Test
    fun `metal edge factories apply gas and wall conduction independently`() {
        val config = ClockworkConfig.KELVIN
        val gasBefore = config.ductThermalConductivityMultiplier
        val wallBefore = config.ductWallThermalConductance
        try {
            config.ductThermalConductivityMultiplier = 37.0
            config.ductWallThermalConductance = 12.5
            val a = DuctNodePos(0.0, 0.0, 0.0)
            val b = DuctNodePos(1.0, 0.0, 0.0)
            val edges = listOf(
                DuctNetworkUtils.createPipeEdge(a, b),
                DuctNetworkUtils.createOneWayEdge(a, b),
                DuctNetworkUtils.createFilteredEdge(a, b),
                DuctNetworkUtils.createSmartEdge(a, b),
                DuctNetworkUtils.configureMetalDuctEdge(ClockworkFilteredOnewayDuctEdge(ConnectionType.PIPE, a, b)),
            )
            for (edge in edges) {
                assertEquals(37.0, edge.thermalConductivityMultiplier, 0.0)
                assertEquals(12.5, edge.wallThermalConductance, 0.0)
                val tag = edge.serialize(CompoundTag())
                edge.wallThermalConductance = 0.0
                edge.deserialize(tag)
                assertEquals(12.5, edge.wallThermalConductance, 0.0)

                // DuctBlockEntity restores saved edge settings before applying current config.
                config.ductThermalConductivityMultiplier = 0.0
                config.ductWallThermalConductance = 8.0
                DuctNetworkUtils.configureMetalDuctEdge(edge)
                assertEquals(0.0, edge.thermalConductivityMultiplier, 0.0)
                assertEquals(8.0, edge.wallThermalConductance, 0.0)
            }
        } finally {
            config.ductThermalConductivityMultiplier = gasBefore
            config.ductWallThermalConductance = wallBefore
        }
    }
}
