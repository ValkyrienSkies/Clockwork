package org.valkyrienskies.clockwork.platform.api.network

interface C2SCWPacket : CWPacket {
    fun handle(context: ServerNetworkContext)
}
