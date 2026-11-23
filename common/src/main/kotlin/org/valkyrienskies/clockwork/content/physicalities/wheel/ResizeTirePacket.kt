package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.mod.common.toWorldCoordinates

internal class ResizeTirePacket(private val pos: BlockPos, private val hand: InteractionHand, private val bodyId: Long,
                               private val mode: AirPumpSetting, private val previous: Int, private val selected: Int) : C2SCWPacket {
    constructor(buf: FriendlyByteBuf) : this(buf.readBlockPos(), buf.readEnum(InteractionHand::class.java),
        buf.readLong(), buf.readEnum(AirPumpSetting::class.java), buf.readVarInt(), buf.readVarInt())

    override fun write(buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeEnum(hand)
        buf.writeLong(bodyId)
        buf.writeEnum(mode)
        buf.writeVarInt(previous)
        buf.writeVarInt(selected)
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            val world = player.level()
            val stack = player.getItemInHand(hand)
            val pump = stack.item as? AirPumpItem ?: return@enqueueWork
            if (mode == AirPumpSetting.PRESSURE || pump.setting(stack) != mode ||
                player.cooldowns.isOnCooldown(pump) || !player.mayBuild() || player.isSpectator ||
                !world.isLoaded(pos) || !world.mayInteract(player, pos) ||
                player.distanceToSqr(world.toWorldCoordinates(pos.center)) > 64) return@enqueueWork
            val hub = world.getBlockEntity(pos) as? WheelHubBlockEntity ?: return@enqueueWork
            val tire = hub.tire ?: return@enqueueWork
            if ((bodyId >= 0 && hub.bodyId != bodyId) || mode.value(tire) != previous) {
                player.displayClientMessage(Component.translatable("item.vs_clockwork.air_pump.size_changed"), true)
                return@enqueueWork
            }
            if (selected in mode.range(tire) && hub.setTireSetting(mode, selected, player))
                pump.finishStroke(player, hand, hub, selected < previous)
        }
        context.setPacketHandled(true)
    }
}
