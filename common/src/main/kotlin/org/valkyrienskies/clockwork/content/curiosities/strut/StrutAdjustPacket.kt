package org.valkyrienskies.clockwork.content.curiosities.strut

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringControls
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringSettings
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeRiding
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import java.util.UUID

class StrutAdjustPacket(private val id: UUID, private val direction: Int) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(buffer.readUUID(), buffer.readByte().toInt())

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUUID(id)
        buffer.writeByte(direction)
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            if (direction != 1 && direction != -1 || !player.isAlive || !player.mayBuild() || player.isSpectator) return@enqueueWork
            val level = player.serverLevel()
            val links = WandLinks.get(level)
            val link = links.links[id]?.takeIf { it.strut && !it.suspended && it.a.shipId != it.b.shipId } ?: return@enqueueWork
            val tool = player.mainHandItem.takeIf { SpringControls.isTool(it) }
                ?: player.offhandItem.takeIf { SpringControls.isTool(it) } ?: return@enqueueWork
            if (!level.mayInteract(player, link.a.pos) || !level.mayInteract(player, link.b.pos) ||
                link.lastAdjustment == level.gameTime || pick(player, link.a, link.b, WandRopeRiding.visibleRange(player)) == null) return@enqueueWork
            if (SpringControls.isWrench(tool)) {
                StrutPhysics.resize(link, SpringSettings.adjustLength(link.length, direction, player.isShiftKeyDown))
            } else {
                val previous = link.pose0
                StrutPhysics.nudge(level, link, StrutPhysics.axis(level, link.a, player.lookAngle), direction, player.isShiftKeyDown)
                val length = StrutPhysics.targetLength(level, link)
                if (length !in 0.5..128.0) {
                    link.pose0 = previous
                    return@enqueueWork
                }
                link.length = length
            }
            link.lastAdjustment = level.gameTime
            link.settingsDirty = true
            links.setDirty()
            WanderwandServer.syncLinks(level)
        }
        context.setPacketHandled(true)
    }

    companion object {
        internal fun pick(player: Player, a: WandAnchor, b: WandAnchor, range: Double): Double? {
            val level = player.level()
            val start = a.world(level, level.isClientSide) ?: return null
            val end = b.world(level, level.isClientSide) ?: return null
            return WandRopeCurve(start, end, 0.0).pick(player.eyePosition, player.lookAngle, range)?.distance
        }
    }
}
