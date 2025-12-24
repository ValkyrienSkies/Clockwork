package org.valkyrienskies.clockwork.client.render.airpocket

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.mod.api.shipWorld
import org.valkyrienskies.mod.api.vsApi

class AirpocketSyncPacket: C2SCWPacket {

    private val nodes: MutableSet<BlockPos>

    constructor(nodes: MutableSet<BlockPos>) {
        this.nodes = nodes
    }

    constructor(buffer: FriendlyByteBuf) {
        val nodes = mutableSetOf<BlockPos>()
        val amount = buffer.readInt()

        //println("AMOUNT: $amount")
        for (x in 1..amount)
            nodes.add(buffer.readBlockPos())

        this.nodes = nodes
    }


    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            AirpocketRenderer.Nodes = nodes
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeInt(nodes.size)
        nodes.forEach { buffer.writeBlockPos(it) }
    }
}
