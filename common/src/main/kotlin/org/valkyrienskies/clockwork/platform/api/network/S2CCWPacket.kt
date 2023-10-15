package org.valkyrienskies.clockwork.platform.api.network

interface S2CCWPacket : CWPacket {
    fun handle(context: ClientNetworkContext)
}
