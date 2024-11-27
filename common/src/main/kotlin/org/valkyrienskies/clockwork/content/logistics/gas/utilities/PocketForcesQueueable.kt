package org.valkyrienskies.clockwork.content.logistics.gas.utilities

import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.kelvin.api.GasType
import java.util.*

data class PocketForcesQueueable(val rootPos: Vector3ic, val pocketCenter: Vector3dc, val pocketSize: Long, val gasMasses: EnumMap<GasType, Double>, val temperature: Double, val pressure: Double)
