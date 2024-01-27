package org.valkyrienskies.clockwork.kelvin.api

interface GasGraph {
    fun tick(timeStep: Double, subSteps: Int): GasSimResultFrame

    fun queueChanges(changesFrame: GasSimChangesFrame)
}