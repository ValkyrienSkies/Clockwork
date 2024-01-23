package org.valkyrienskies.kelvin

interface GasGraph {
    fun tick(timeStep: Double, subSteps: Int): GasSimResultFrame

    fun queueChanges(changesFrame: GasSimChangesFrame)
}