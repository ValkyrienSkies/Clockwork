package org.valkyrienskies.clockwork.effekseer.common.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.ParticleEmitter
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

@ApiStatus.Experimental
class EmitterTriggerPacket: S2CCWPacket {

    override var player: Player? = null
    private val type: ParticleEmitter.Type
    private val effek: ResourceLocation
    private val emitterName: ResourceLocation
    private val triggers: IntArray

    constructor(
        type: ParticleEmitter.Type,
        effek: ResourceLocation,
        emitterName: ResourceLocation,
        triggers: IntArray
    ) {
        this.type = type
        this.effek = effek
        this.emitterName = emitterName
        this.triggers = triggers
    }

    constructor(buf: FriendlyByteBuf) {
        type = buf.readEnum(ParticleEmitter.Type::class.java)
        effek = buf.readResourceLocation()
        emitterName = buf.readResourceLocation()
        triggers = buf.readVarIntArray()
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            ClockworkModClient.sendTrigger(type, effek, emitterName, triggers)
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeEnum(type)
        buffer.writeResourceLocation(effek)
        buffer.writeResourceLocation(emitterName)
        buffer.writeVarIntArray(triggers)
    }
}