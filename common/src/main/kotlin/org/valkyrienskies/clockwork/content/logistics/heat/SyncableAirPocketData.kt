package org.valkyrienskies.clockwork.content.logistics.heat

import org.joml.Vector3ic
import org.valkyrienskies.core.api.datastructures.dynconn.BlockPosVertex

data class SyncableAirPocketData(
    val shipId: Long,
    val pocketId: Int,
    val airPockets: HashMap<Vector3ic, BlockPosVertex>
)

