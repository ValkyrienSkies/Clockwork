package org.valkyrienskies.clockwork.platform.api.network

import dev.architectury.networking.NetworkManager.PacketContext

interface NetworkContext {
    fun enqueueWork(runnable: Runnable)
    fun handled() {
        setPacketHandled(true)
    }

    fun setPacketHandled(value: Boolean)
}