package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class BalloonStatusPacket : S2CCWPacket {
    override var player: Player? = null
    private val leaks: List<Pair<Vector3ic, Boolean>>
    private val sealedLeaks: List<Vector3ic>

    constructor(buffer: FriendlyByteBuf) {
        leaks = readLeaks(buffer)
        sealedLeaks = readSealedLeaks(buffer)
    }

    constructor(balloon: BalloonData) {
        this.sealedLeaks = balloon.lastSentLeaks.filter { leakPos ->
            !balloon.leakPositions.contains(leakPos)
        }
        this.leaks = run {
            val leaks = balloon.leakPositions
            val prevLeaks = balloon.lastSentLeaks
            val newList = ArrayList<Pair<Vector3ic, Boolean>>()
            for (leak in leaks) {
                val isNew = !prevLeaks.contains(leak)
                newList.add(Pair(leak, isNew))
            }
            prevLeaks.clear()
            prevLeaks.addAll(leaks)
            newList
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        writeLeaks(buffer)
        writeSealedLeaks(buffer)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null
            ) {
                val level = Minecraft.getInstance().level!!
                for (leak in leaks) {
                    val pos = leak.first
                    val isNew = leak.second

                    ClockworkModClient.CLIENT_BALLOON_LEAKS.add(pos)
                    if (isNew) {
                        //inital particle burst
                    }
                }
            }
        }
        context.setPacketHandled(true)
    }

    private fun writeLeaks(buffer: FriendlyByteBuf) {
        buffer.writeInt(leaks.size)
        for (leak in leaks) {
            buffer.writeInt(leak.first.x())
            buffer.writeInt(leak.first.y())
            buffer.writeInt(leak.first.z())
            buffer.writeBoolean(leak.second)
        }
    }

    private fun readLeaks(buffer: FriendlyByteBuf): List<Pair<Vector3ic, Boolean>> {
        val size = buffer.readInt()
        val leaks = mutableListOf<Pair<Vector3ic, Boolean>>()
        for (i in 0 until size) {
            val x = buffer.readInt()
            val y = buffer.readInt()
            val z = buffer.readInt()
            val isNew = buffer.readBoolean()
            leaks.add(Pair(Vector3i(x, y, z), isNew))
        }
        return leaks
    }

    private fun writeSealedLeaks(buffer: FriendlyByteBuf) {
        buffer.writeInt(sealedLeaks.size)
        for (leak in sealedLeaks) {
            buffer.writeInt(leak.x())
            buffer.writeInt(leak.y())
            buffer.writeInt(leak.z())
        }
    }

    private fun readSealedLeaks(buffer: FriendlyByteBuf): List<Vector3ic> {
        val size = buffer.readInt()
        val leaks = mutableListOf<Vector3ic>()
        for (i in 0 until size) {
            val x = buffer.readInt()
            val y = buffer.readInt()
            val z = buffer.readInt()
            leaks.add(Vector3i(x, y, z))
        }
        return leaks
    }
}
