package org.valkyrienskies.clockwork.platform.api.network

interface NetworkContext {
    fun enqueueWork(runnable: Runnable)
    fun handled() {
        setPacketHandled(true)
    }

    fun setPacketHandled(value: Boolean)
}