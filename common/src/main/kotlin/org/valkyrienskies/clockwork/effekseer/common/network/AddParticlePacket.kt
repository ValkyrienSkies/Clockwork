package org.valkyrienskies.clockwork.effekseer.common.network

import dev.architectury.networking.NetworkManager.PacketContext
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.clockwork.effekseer.api.common.ParticleEmitterInfo
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket


class AddParticlePacket : ParticleEmitterInfo, S2CCWPacket {

    override var player: Player? = null

    @ApiStatus.Internal
    constructor(effek: ResourceLocation?) : super (effek) {

    }

    /**
     * @see .create
     */
    @ApiStatus.Internal
    constructor(effek: ResourceLocation?, emitter: ResourceLocation?) : super(effek, emitter) {

    }

    constructor(buf: FriendlyByteBuf) : super(buf) {

    }

    constructor(toCopy: ParticleEmitterInfo) : super(toCopy.effek, toCopy.emitter) {
        toCopy.copyTo(this)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (player != null) {
                spawnInWorld(player!!.level, player)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        super.encode(buffer)
    }
}