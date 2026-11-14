package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class AeronautGogglesTogglePacket() : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this()

    override fun write(buffer: FriendlyByteBuf) {}

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            if (!player.isAlive || player.isSpectator ||
                player.getItemBySlot(EquipmentSlot.HEAD).item !is AeronautGogglesItem) return@enqueueWork

            val state = player as MixinPlayerDuck
            state.gogglesDown = !state.gogglesDown
            // Entity data sends the new state to the wearer and all tracking clients,
            // including clients that start tracking this player after the toggle.
            val sound = if (state.gogglesDown) ClockworkSounds.GOGGLES_EQUIP else ClockworkSounds.GOGGLES_UNEQUIP
            player.level().playSound(null, player.blockPosition(), sound.mainEvent!!, SoundSource.PLAYERS, 1f, 1f)
        }
        context.setPacketHandled(true)
    }
}
