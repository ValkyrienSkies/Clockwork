//package org.valkyrienskies.clockwork.client.render.airpocket
//
//import net.minecraft.core.BlockPos
//import net.minecraft.network.FriendlyByteBuf
//import org.joml.Vector3d
//import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
//import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
//import org.valkyrienskies.mod.api.shipWorld
//import org.valkyrienskies.mod.api.vsApi
//
//class AirpocketSyncPacket: C2SCWPacket {
//
//
//    constructor(buffer: FriendlyByteBuf) {
//    }
//
//    constructor(position: Vector3d) {
//
//    }
//
//    override fun handle(context: ServerNetworkContext) {
//        context.enqueueWork {
//            val level = context.sender.level()
//            level.shipWorld!!.loadedShips.forEach { it.chunkClaim. }
//        }
//    }
//
//    override fun write(buffer: FriendlyByteBuf) {
//    }
//}