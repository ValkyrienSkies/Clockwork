package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class GravitronInputPacket(val action: Action, val shipId: Long = -1, val x: Float = 0f, val y: Float = 0f) : C2SCWPacket {
    enum class Action { ROTATE, DISTANCE, CONNECTED_MASS }
    constructor(buffer: FriendlyByteBuf) : this(buffer.readEnum(Action::class.java), buffer.readLong(), buffer.readFloat(), buffer.readFloat())
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeEnum(action); buffer.writeLong(shipId); buffer.writeFloat(x); buffer.writeFloat(y)
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { apply(context.sender) }
        context.setPacketHandled(true)
    }

    fun apply(player: ServerPlayer) {
        if (!x.isFinite() || !y.isFinite() || !GrabTool.updateEquipment(player)) return
        val state = GravitronState.getState(player)
        if (action != Action.CONNECTED_MASS && state.shipID != shipId) return
        val tick = player.level().gameTime
        if (state.lastInputTicks[action.ordinal] == tick) return
        state.lastInputTicks[action.ordinal] = tick
        val stack = player.mainHandItem
        if (action == Action.CONNECTED_MASS) {
            val enabled = !GravitronLoad.enabled(stack)
            stack.orCreateTag.putBoolean(GravitronLoad.CONNECTED_MASS, enabled)
            (if (enabled) ClockworkSounds.GRAVITRON_MASSMODE_ON else ClockworkSounds.GRAVITRON_MASSMODE_OFF)
                .playOnServer(player.serverLevel(), player.blockPosition(), 0.7f, 1f)
            player.displayClientMessage(Component.translatable("vs_clockwork.gravitron.connected_mass." + if (enabled) "on" else "off"), true)
            return
        }
        when (action) {
            Action.ROTATE -> {
                val rotation = state.shipGrabbedRot ?: return
                val reference = state.playerGrabbedRotation ?: return
                state.shipGrabbedRot = GravitronManipulation.rotate(rotation, reference, x.toDouble(), y.toDouble())
            }
            Action.DISTANCE -> {
                val distance = state.shipGrabbedDistance ?: return
                val maximum = if (stack.item is CreativeGravitronItem) GravitronManipulation.CREATIVE_MAX_DISTANCE
                    else ClockworkConfig.SERVER.survivalGravitronMaxRange
                state.shipGrabbedDistance = GravitronManipulation.distance(distance, x.toDouble(), maximum)
            }
            else -> Unit
        }
    }
}
