package org.valkyrienskies.clockwork.content.logistics.gas.utilities

import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.kelvin.api.GasType
import java.util.*
import kotlin.collections.HashMap

data class PocketForcesQueueable(val rootPos: Vector3ic, val pocketCenter: Vector3dc, val pocketSize: Long, val gasMasses: HashMap<GasType, Double>, val temperature: Double, val pressure: Double)
