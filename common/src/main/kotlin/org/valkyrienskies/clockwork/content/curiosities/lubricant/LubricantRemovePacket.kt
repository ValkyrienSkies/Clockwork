package org.valkyrienskies.clockwork.content.curiosities.lubricant

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.mod.common.world.clipIncludeShips

class LubricantRemovePacket() : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this()
    override fun write(buffer: FriendlyByteBuf) {}
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { remove(context.sender) }
        context.setPacketHandled(true)
    }

    companion object {
        fun remove(player: ServerPlayer) {
            val stack = player.mainHandItem
            if (stack.item !is LubricantItem || !player.mayBuild() || player.isSpectator || !player.isAlive) return
            if (stack.tag?.contains("Start") == true) {
                LubricantItem.clear(stack)
                return
            }
            if (player.cooldowns.isOnCooldown(stack.item)) return
            val level = player.serverLevel()
            val eye = player.eyePosition
            val hit = level.clipIncludeShips(ClipContext(eye, eye.add(player.lookAngle.scale(if (player.isCreative) 5.0 else 4.5)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player), true)
            if (hit.type != HitResult.Type.BLOCK) return
            val count = LubricantData.get(level).removeArea(level, hit.blockPos) { level.mayInteract(player, it) }
            if (count == 0) return
            player.cooldowns.addCooldown(stack.item, 5)
            ClockworkSounds.LUBRICANT_REMOVE.playOnServer(level, player.blockPosition(), 0.8f, 0.95f + level.random.nextFloat() * 0.1f)
            player.displayClientMessage(Component.translatable("item.vs_clockwork.lubricant.removed", count), true)
        }
    }
}
