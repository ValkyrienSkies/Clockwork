package org.valkyrienskies.clockwork.content.logistics.gas.utilities

import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.kelvin.api.GasType
import java.util.*
import kotlin.collections.HashMap

data class PocketForcesQueueable(val pocketCenter: Vector3dc, val pocketVolume: Double, val hotDensity: Double)
