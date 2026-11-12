package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotPhysics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer

object ThinkableConnections {
    data class Input(val value: Double?)
    data class Source(val device: IThinkable?, val side: Direction)

    fun input(level: Level?, pos: BlockPos, side: Direction): Input? {
        val source = inputSource(level, pos, side) ?: return null
        return Input(source.device?.thinkableValue(source.side)?.takeIf { it.isFinite() })
    }

    fun inputSource(level: Level?, pos: BlockPos, side: Direction): Source? {
        val world = level as? ServerLevel ?: return null
        val sink = WandAnchor.at(world, pos, side)
        if (device(world, sink)?.acceptsThinkable(side) != true) return null
        incoming(world, sink)?.let { link ->
            val source = if (link.suspended) null else device(world, link.a)?.takeIf { it.providesThinkable(link.a.face) }
            return Source(source, link.a.face)
        }
        val source = WandAnchor.at(world, pos.relative(side), side.opposite)
        if (source.shipId != sink.shipId) return null
        val device = device(world, source)?.takeIf { it.providesThinkable(source.face) } ?: return null
        return Source(device, source.face)
    }

    fun device(level: ServerLevel, anchor: WandAnchor): IThinkable? {
        if (!level.hasChunkAt(anchor.pos) || anchor.world(level) == null ||
            WandAnchor.at(level, anchor.pos, anchor.face).shipId != anchor.shipId) return null
        val entity = level.getBlockEntity(anchor.pos) ?: return null
        if (entity.isRemoved || !entity.type.isValid(level.getBlockState(anchor.pos))) return null
        return entity as? IThinkable
    }

    fun incoming(level: ServerLevel, sink: WandAnchor): WandLinks.Link? =
        WandLinks.get(level).links.values.firstOrNull { it.thinkable && it.b == sink }

    fun read(level: ServerLevel, sink: WandAnchor): Double? {
        val link = incoming(level, sink)?.takeUnless { it.suspended } ?: return null
        return ThinkableItem.source(level, link.a)
    }

    fun connect(level: ServerLevel, source: WandAnchor, sink: WandAnchor): Boolean {
        if (source.pos == sink.pos || incoming(level, sink) != null ||
            device(level, source)?.providesThinkable(source.face) != true ||
            device(level, sink)?.acceptsThinkable(sink.face) != true) return false
        val start = source.world(level) ?: return false
        val end = sink.world(level) ?: return false
        val distance = start.distanceTo(end)
        if (distance > ThinkableItem.MAX_RANGE) return false
        WandLinks.get(level).add(level, source, sink, true, RopeKnotPhysics.placedLength(distance),
            physical = true, thinkable = true) ?: return false
        WanderwandServer.syncLinks(level)
        return true
    }

    fun disconnect(level: ServerLevel, sink: WandAnchor): Boolean {
        val link = incoming(level, sink) ?: return false
        WandLinks.get(level).remove(level, link)
        sink.world(level)?.let { ClockworkSounds.THINKABLE_ATTACH.play(level, null, it, 0.6f, 0.8f) }
        WanderwandServer.syncLinks(level)
        return true
    }
}
