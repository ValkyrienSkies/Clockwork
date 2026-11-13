package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.api.thinkable.Thinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableContext
import org.valkyrienskies.clockwork.compat.thinkable.ThinkableCompat
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotPhysics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer

object ThinkableConnections {
    data class Input(val value: Double?)
    data class Source(val device: Thinkable?, val side: Direction)

    fun input(level: Level?, pos: BlockPos, side: Direction): Input? {
        val source = inputSource(level, pos, side) ?: return null
        val reading = level?.let { device(it, pos)?.thinkableInputReading(side) } ?: ""
        return Input(source.device?.thinkableValue(source.side, reading)?.takeIf { it.isFinite() })
    }

    fun inputSource(level: Level?, pos: BlockPos, side: Direction): Source? {
        val world = level as? ServerLevel ?: return null
        val sink = WandAnchor.at(world, pos, side)
        if (device(world, sink)?.acceptsThinkable(side) != true) return null
        return sourceAt(world, pos, side)
    }

    fun sourceAt(world: ServerLevel, pos: BlockPos, side: Direction, cableOnly: Boolean = false): Source? {
        val sink = WandAnchor.at(world, pos, side)
        incoming(world, sink)?.let { link ->
            val source = if (link.suspended) null else device(world, link.a)?.takeIf { it.providesThinkable(link.a.face) }
            return Source(source, link.a.face)
        }
        if (cableOnly) return null
        val source = WandAnchor.at(world, pos.relative(side), side.opposite)
        if (source.shipId != sink.shipId) return null
        val device = device(world, source)?.takeIf { it.providesThinkable(source.face) } ?: return null
        return Source(device, source.face)
    }

    fun device(level: ServerLevel, anchor: WandAnchor): Thinkable? {
        if (!level.hasChunkAt(anchor.pos) || anchor.world(level) == null ||
            WandAnchor.at(level, anchor.pos, anchor.face).shipId != anchor.shipId) return null
        return device(level, anchor.pos)
    }

    fun device(level: Level, pos: BlockPos): Thinkable? {
        if (!level.hasChunkAt(pos)) return null
        val state = level.getBlockState(pos)
        val entity = level.getBlockEntity(pos)?.takeUnless { it.isRemoved || !it.type.isValid(state) }
        val context = ThinkableContext(level, pos, state, entity)
        ThinkableCompat.find(context)?.let { return adapted(context, it) }
        if (entity is Thinkable) return entity
        val block = state.block as? IThinkableBlock ?: return null
        val position = pos.immutable()
        return object : IThinkable {
            private fun currentState() = if (level.hasChunkAt(position))
                level.getBlockState(position).takeIf { it.block === block } else null

            override fun providesThinkable(side: Direction): Boolean =
                currentState()?.let { block.providesThinkable(it, side) } == true

            override fun thinkableValue(side: Direction): Double? {
                val world = level as? ServerLevel ?: return null
                val current = currentState() ?: return null
                if (!block.providesThinkable(current, side)) return null
                return block.thinkableValue(current, world, position, side)?.takeIf { it.isFinite() }
            }

            override fun thinkablePortName(side: Direction) =
                currentState()?.let { block.thinkablePortName(it, side) }
        }
    }

    private fun adapted(context: ThinkableContext, delegate: Thinkable): Thinkable = object : Thinkable {
        private fun valid() = context.level().hasChunkAt(context.pos()) &&
            context.level().getBlockState(context.pos()) == context.state() &&
            context.level().getBlockEntity(context.pos()) === context.blockEntity() &&
            context.blockEntity()?.isRemoved != true

        override fun providesThinkable(side: Direction) = valid() && delegate.providesThinkable(side)
        override fun acceptsThinkable(side: Direction) = valid() && delegate.acceptsThinkable(side)
        override fun thinkablePortName(side: Direction) = if (valid()) delegate.thinkablePortName(side) else null
        override fun thinkableReadings(side: Direction) = if (providesThinkable(side)) delegate.thinkableReadings(side) else emptyList()
        override fun thinkableInputReading(side: Direction) = if (acceptsThinkable(side)) delegate.thinkableInputReading(side) else ""

        override fun thinkableValue(side: Direction): Double? =
            if (context.level() is ServerLevel && providesThinkable(side)) delegate.thinkableValue(side)?.takeIf { it.isFinite() } else null

        override fun thinkableValue(side: Direction, reading: String): Double? =
            if (context.level() is ServerLevel && providesThinkable(side)) delegate.thinkableValue(side, reading)?.takeIf { it.isFinite() } else null

        override fun receiveThinkable(side: Direction, value: Double?) {
            if (context.level() is ServerLevel && acceptsThinkable(side)) delegate.receiveThinkable(side, value?.takeIf { it.isFinite() })
        }

        override fun onThinkablePulse(side: Direction) {
            if (context.level() is ServerLevel && acceptsThinkable(side)) delegate.onThinkablePulse(side)
        }
    }

    fun sendAdjacent(level: ServerLevel, pos: BlockPos, side: Direction, value: Double?, clear: Boolean = false) {
        val sink = WandAnchor.at(level, pos.relative(side), side.opposite)
        if (WandAnchor.at(level, pos, side).shipId != sink.shipId || incoming(level, sink) != null) return
        val receiver = device(level, sink) ?: return
        if (receiver.acceptsThinkable(sink.face)) {
            val reading = receiver.thinkableInputReading(sink.face)
            val selected = if (clear) null else if (reading.isEmpty()) value else device(level, pos)?.thinkableValue(side, reading)
            receiver.receiveThinkable(sink.face, selected?.takeIf { it.isFinite() })
        }
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
