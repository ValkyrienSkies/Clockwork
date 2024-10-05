package org.valkyrienskies.clockwork.effekseer.api.common

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import org.jetbrains.annotations.ApiStatus
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.ParticleEmitter
import org.valkyrienskies.clockwork.effekseer.common.network.AddParticlePacket
import org.valkyrienskies.clockwork.effekseer.common.network.EmitterTriggerPacket
import org.valkyrienskies.clockwork.effekseer.common.network.UpdateEmitterParameterPacket


class EffekseerLevel {
    fun addParticle(level: Level, info: ParticleEmitterInfo) {
        addParticle(level, false, info)
    }

    fun addParticle(level: Level, force: Boolean, info: ParticleEmitterInfo) {
        addParticle(level, (if (force) 512 else 32).toDouble(), info)
    }

    fun addParticle(level: Level, distance: Double, info: ParticleEmitterInfo) {
        if (level.isClientSide()) {
            ClockworkModClient.addParticle(level, info)
        } else {
            val packet = AddParticlePacket(info)
            val serverLevel = level
            val sqrDistance = distance * distance
            for (player in serverLevel.players()) {
                sendToPlayer(player as ServerPlayer, serverLevel, packet, sqrDistance)
            }
        }
    }

    @ApiStatus.Experimental
    fun setParameterFor(
        player: Player,
        type: ParticleEmitter.Type,
        effek: ResourceLocation,
        emitterName: ResourceLocation,
        parameters: Array<DynamicParameter>
    ) {
        if (player.level.isClientSide) {
            ClockworkModClient.setParam(type, effek, emitterName, parameters)
        } else {
            ClockworkPackets.sendTo(UpdateEmitterParameterPacket(type, effek, emitterName, parameters), player as ServerPlayer)
        }
    }

    @ApiStatus.Experimental
    fun sendTriggerFor(
        player: Player,
        type: ParticleEmitter.Type,
        effek: ResourceLocation,
        emitterName: ResourceLocation,
        triggers: IntArray
    ) {
        if (player.level.isClientSide) {
            ClockworkModClient.sendTrigger(type, effek, emitterName, triggers)
        } else {
            ClockworkPackets.sendTo(EmitterTriggerPacket(type, effek, emitterName, triggers), player as ServerPlayer)
        }
    }

    private fun sendToPlayer(player: ServerPlayer, level: Level, packet: AddParticlePacket, sqrDistance: Double) {
        if (player.getLevel() !== level) {
            return
        }
        if (packet.isPositionSet()) {
            if (player.position().distanceToSqr(packet.position()) > sqrDistance) {
                return
            }
        }
        ClockworkPackets.sendTo(packet, player)
    }
}