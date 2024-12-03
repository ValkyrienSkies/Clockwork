package org.valkyrienskies.clockwork.kelvin.impl.client

import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.kelvin.impl.DuctNodeInfo

data class ClientKelvinInfo(val nodes: HashMap<DuctNodePos, DuctNodeInfo>) {
}
