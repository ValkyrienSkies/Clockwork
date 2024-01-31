package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.simibubi.create.foundation.outliner.Outliner
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML

class WanderWandClearPacket : S2CCWPacket {

    constructor(buffer: FriendlyByteBuf) {

    }

    constructor() {

    }

    override fun write(buffer: FriendlyByteBuf) {

    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                if (Minecraft.getInstance().player!!.mainHandItem.item !is WanderWandItem) {
                    context.setPacketHandled(true)
                    return@enqueueWork
                }
                val adi = Minecraft.getInstance().player!!.mainHandItem.item as WanderWandItem
                adi.firstPos = null
                val copy: Map<Any, Outliner.OutlineEntry> = HashMap(ClockworkModClient.WANDER_OUTLINER.outlines)
                for ((key) in copy) {
                    ClockworkModClient.WANDER_OUTLINER.remove(key)
                }
            }
        }
        context.setPacketHandled(true)
    }
}