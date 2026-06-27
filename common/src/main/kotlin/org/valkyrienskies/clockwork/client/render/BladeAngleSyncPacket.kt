package org.valkyrienskies.clockwork.client.render

import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class BladeAngleSyncPacket(private val entityId: Int, private val newAngle: Double) : S2CCWPacket {
    override var player: Player? = null

    constructor(buffer: FriendlyByteBuf) : this(buffer.readInt(), buffer.readDouble()) {

    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level == null) return@enqueueWork
            val entity = Minecraft.getInstance().level!!.getEntity(entityId)
            if (entity !is ControlledContraptionEntity) return@enqueueWork

            val blocks = entity.contraption.blocks
            for ((_, value) in blocks) {
                if (value.state.`is`(ClockworkBlocks.BLADE_CONTROLLER.get())) {
                    value.nbt?.putDouble("BladeAngle", newAngle)
                    break
                }
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeInt(entityId)
        buffer.writeDouble(newAngle)
    }
}