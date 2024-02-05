package org.valkyrienskies.clockwork.platform.api.network

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import java.util.function.Function

interface PacketChannel {
    fun <T : CWPacket> registerPacket(clazz: Class<T>, decode: Function<FriendlyByteBuf, T>)
    fun sendToNear(world: Level, pos: BlockPos, range: Int, message: S2CCWPacket)
    fun sendToServer(packet: C2SCWPacket)
    fun sendToClientsTracking(packet: S2CCWPacket, entity: Entity)
    fun sendToClientsTrackingAndSelf(packet: S2CCWPacket, player: ServerPlayer)
    fun sendTo(packet: S2CCWPacket, player: ServerPlayer)
}