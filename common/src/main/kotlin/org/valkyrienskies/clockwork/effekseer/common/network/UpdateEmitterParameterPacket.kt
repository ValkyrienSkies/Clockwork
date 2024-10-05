package org.valkyrienskies.clockwork.effekseer.common.network

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.ParticleEmitter
import org.valkyrienskies.clockwork.effekseer.api.common.DynamicParameter
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket


@ApiStatus.Experimental
class UpdateEmitterParameterPacket: S2CCWPacket {
    private val type: ParticleEmitter.Type
    private val effek: ResourceLocation
    private val emitterName: ResourceLocation
    private val parameters: Array<DynamicParameter>

    constructor(
        type: ParticleEmitter.Type,
        effek: ResourceLocation,
        emitterName: ResourceLocation,
        parameters: Array<DynamicParameter>
    ) {
        this.type = type
        this.effek = effek
        this.emitterName = emitterName
        this.parameters = parameters
    }

    constructor(buf: FriendlyByteBuf) {
        type = buf.readEnum(ParticleEmitter.Type::class.java)
        effek = buf.readResourceLocation()
        emitterName = buf.readResourceLocation()

        val referenceArray = arrayOfNulls<DynamicParameter>(buf.readVarInt())
        for (i in referenceArray.indices) {
            val index = buf.readVarInt()
            val value = buf.readFloat()
            referenceArray[i] = DynamicParameter(index, value)
        }
        parameters = referenceArray.requireNoNulls()
    }


    override var player: Player? = null

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            ClockworkModClient.setParam(type, effek, emitterName, parameters)
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeEnum(type)
        buffer.writeResourceLocation(effek)
        buffer.writeResourceLocation(emitterName)

        buffer.writeVarInt(parameters.size)
        for (parameter in parameters) {
            buffer.writeVarInt(parameter!!.index)
            buffer.writeFloat(parameter.value)
        }
    }
}