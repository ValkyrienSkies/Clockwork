package org.valkyrienskies.clockwork.content.curiosities.tools.bluper

import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.util.ClockworkUtils

class BluperHelperPacket : S2CCWPacket {
    private val boxSet: Set<AABBic>?

    constructor(buffer: FriendlyByteBuf) {
        val set: Set<AABBic>
        val nbtList = buffer.readNbt()
        if (nbtList!!.contains("Box0")) {
            var size = buffer.readVarInt()
            for (i in 0..size) {
                val nbt = nbtList.get("Box$i")
                //ClockworkUtils.readAABBi(nbt)
            }
        }



        boxSet = null
    }


    constructor(cluster: Set<AABBic>){
        this.boxSet = cluster
    }

    override fun write(buffer: FriendlyByteBuf) {
        val tag = CompoundTag()
        var i = 0
        boxSet!!.forEach { box ->
            val list = ClockworkUtils.writeAABBi(box)
            tag.put("Box$i", list)
            i++
        }
        buffer.writeVarInt(i)
        buffer.writeNbt(tag)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                ClockworkMod.OUTLINER.remove(boxSet)
            }
        }
        context.setPacketHandled(true)
    }
}