package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.world.clipIncludeShips
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

internal data class GatemakyrRayPath(val gates: List<GatemakyrPlayerGate>, val distance: Double, val firstGateDistance: Double, val crossings: List<Pair<Vec3, Vec3>> = emptyList())

internal interface GatemakyrHit {
    var gatePath: GatemakyrRayPath?
}

object GatemakyrRaycast {
    private class Segment(from: Vec3, to: Vec3, val original: ClipContext) :
        ClipContext(from, to, Block.COLLIDER, Fluid.NONE, null) {
        override fun getBlockShape(state: BlockState, level: BlockGetter, pos: BlockPos) = original.getBlockShape(state, level, pos)
        override fun getFluidShape(state: FluidState, level: BlockGetter, pos: BlockPos) = original.getFluidShape(state, level, pos)
    }

    internal fun intersection(frame: GatemakyrTarget.Frame, target: GatemakyrTarget, oneSided: Boolean, from: Vec3, delta: Vec3): Double? {
        val relative = from.subtract(frame.position)
        val start = relative.dot(frame.normal)
        val step = delta.dot(frame.normal)
        if (step == 0.0 || oneSided && step >= 0) return null
        val t = -start / step
        if (t <= 0.0 || t > 1.0) return null
        val p = relative.add(delta.scale(t))
        return t.takeIf { abs(p.dot(frame.u)) <= frame.u.lengthSqr() * target.width * 0.5 &&
            abs(p.dot(frame.v)) <= frame.v.lengthSqr() * target.height * 0.5 }
    }

    @JvmStatic fun clip(level: Level, ctx: ClipContext, transform: Boolean, skipShip: Long?, skipWorld: Boolean) =
        trace(level, ctx, !transform) { segment, worldHit -> level.clipIncludeShips(segment, worldHit, skipShip, skipWorld) }

    fun surfaceHit(level: Level, ctx: ClipContext): BlockHitResult =
        trace(level, ctx, true, true) { segment, worldHit -> level.clipIncludeShips(segment, worldHit) }
            ?: level.clipIncludeShips(Segment(ctx.from, loadedEnd(level, ctx.from, ctx.to), ctx), false)

    internal fun flightHit(level: ServerLevel, from: Vec3, to: Vec3): BlockHitResult {
        val ray = ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null)
        return level.clipIncludeShips(Segment(from, loadedEnd(level, from, to), ray), false)
    }

    private fun loadedEnd(level: Level, from: Vec3, to: Vec3): Vec3 {
        var x = floor(from.x / 16).toInt()
        var z = floor(from.z / 16).toInt()
        val endX = floor(to.x / 16).toInt()
        val endZ = floor(to.z / 16).toInt()
        val stepX = endX.compareTo(x)
        val stepZ = endZ.compareTo(z)
        var fraction = 0.0
        while (level.hasChunk(x, z)) {
            if (x == endX && z == endZ) return to
            val tx = if (x == endX) Double.POSITIVE_INFINITY else
                ((x + if (stepX > 0) 1 else 0) * 16.0 - from.x) / (to.x - from.x)
            val tz = if (z == endZ) Double.POSITIVE_INFINITY else
                ((z + if (stepZ > 0) 1 else 0) * 16.0 - from.z) / (to.z - from.z)
            fraction = min(tx, tz)
            if (tx <= tz) x += stepX
            if (tz <= tx) z += stepZ
        }
        return from.lerp(to, (fraction - 1e-6).coerceAtLeast(0.0))
    }

    private fun trace(level: Level, ctx: ClipContext, localHits: Boolean, loadedOnly: Boolean = false, cast: (ClipContext, Boolean) -> BlockHitResult): BlockHitResult? {
        if (ctx is Segment) return null
        val gates = if (level is ServerLevel) {
            if (!level.server.isSameThread) return null
            GatemakyrServer.playerGates(level)
        } else GatemakyrClient.rayGates(level)
        if (gates.isEmpty()) return null
        var from = ctx.from
        var to = ctx.to
        var crossed: MutableList<GatemakyrPlayerGate>? = null
        val crossings = mutableListOf<Pair<Vec3, Vec3>>()
        var traveled = 0.0
        var first = 0.0
        fun missAt(point: Vec3, direction: Vec3): BlockHitResult {
            val hit = BlockHitResult.miss(point, Direction.getNearest(-direction.x, -direction.y, -direction.z), BlockPos.containing(point))
            (hit as GatemakyrHit).gatePath = GatemakyrRayPath(crossed.orEmpty(), traveled, first, crossings)
            return hit
        }
        while (true) {
            val delta = to.subtract(from)
            var nearest = Double.POSITIVE_INFINITY
            var gate: GatemakyrPlayerGate? = null
            for (candidate in gates.values) {
                val t = intersection(candidate.passage.entrance, candidate.source, candidate.source.surface || candidate.destination.surface,
                    from, delta) ?: continue
                if (t < nearest) {
                    nearest = t
                    gate = candidate
                }
            }
            if (gate == null && crossed == null) return null
            val contact = if (gate == null) to else from.add(delta.scale(nearest))
            val end = if (loadedOnly) loadedEnd(level, from, contact) else contact
            val segment = Segment(from, end, ctx)
            val obstruction = cast(segment, !localHits)
            if (gate == null || end !== contact || obstruction.type != HitResult.Type.MISS) {
                if (crossed != null) {
                    val worldHit = if (localHits && obstruction.type == HitResult.Type.BLOCK && level.getShipManagingPos(obstruction.blockPos) != null)
                        cast(segment, true).location else obstruction.location
                    (obstruction as GatemakyrHit).gatePath = GatemakyrRayPath(crossed, traveled + from.distanceTo(worldHit), first, crossings)
                }
                return obstruction
            }
            traveled += from.distanceTo(contact)
            if (crossed == null) {
                first = traveled
                crossed = mutableListOf()
            }
            if (crossed.size == 8) return missAt(contact, delta)
            crossed.add(gate)
            val direction = gate.passage.direction(delta.normalize())
            val exit = gate.passage.point(contact)
            crossings.add(contact to exit)
            val remaining = delta.length() * (1.0 - nearest)
            if (remaining <= 0.00001 || !level.hasChunkAt(BlockPos.containing(exit)))
                return missAt(exit, direction)
            to = exit.add(direction.scale(remaining))
            from = exit.add(direction.scale(0.00001))
            traveled += 0.00001
        }
    }
}
