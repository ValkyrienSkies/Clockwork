package org.valkyrienskies.clockwork.platform.api.network

import net.minecraft.network.FriendlyByteBuf

interface EntityExtraSpawnData {
    fun writeSpawnData(arg: FriendlyByteBuf)
    fun readSpawnData(arg: FriendlyByteBuf)
}