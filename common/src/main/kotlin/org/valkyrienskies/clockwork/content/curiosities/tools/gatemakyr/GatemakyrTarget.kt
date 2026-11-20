package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.world.clipIncludeShips
import kotlin.math.floor
import kotlin.math.round

data class GatemakyrTarget(val position: Vec3, val face: Direction, val shipId: Long = -1, val surface: Boolean = false,
                          val width: Int = 3, val height: Int = 3, val impact: Vec3 = position) {
    data class Frame(val position: Vec3, val u: Vec3, val v: Vec3, val normal: Vec3, val impact: Vec3)

    internal fun localFrame(): Frame {
        val normal = Vec3.atLowerCornerOf(face.normal)
        val up = up(face)
        val right = up.cross(normal)
        return Frame(position, right, up, normal, impact)
    }

    internal fun mountedTransform(level: Level, render: Boolean = false): Matrix4dc? {
        val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: return null
        val canonical = if (render && ship is ClientShip) ship.renderTransform.shipToWorld else ship.shipToWorld
        return GatemakyrShipPieces.transform(level, shipId, position, canonical, render && ship is ClientShip)
    }

    fun frame(level: Level, render: Boolean = false): Frame? {
        val local = localFrame()
        if (shipId < 0) return local
        val transform = mountedTransform(level, render) ?: return null
        fun map(v: Vec3, point: Boolean): Vec3 {
            val out = Vector3d(v.x, v.y, v.z)
            if (point) transform.transformPosition(out) else transform.transformDirection(out)
            return Vec3(out.x, out.y, out.z)
        }
        return Frame(map(position, true), map(local.u, false), map(local.v, false), map(local.normal, false), map(impact, true))
    }

    fun opening(frame: Frame, age: Float, closing: Boolean = false): GatemakyrRiftShape.Bounds {
        val offset = frame.impact.subtract(frame.position)
        return GatemakyrRiftShape.sample(width, height, offset.dot(frame.u) / frame.u.lengthSqr(),
            offset.dot(frame.v) / frame.v.lengthSqr(), age, surface, closing)
    }

    fun fitTo(other: GatemakyrTarget?): GatemakyrTarget {
        if (other == null) return this
        val w = minOf(width, other.width)
        val h = minOf(height, other.height)
        if (w == width && h == height) return this
        if (!surface) return copy(width = w, height = h)
        val v = up(face)
        val u = v.cross(Vec3.atLowerCornerOf(face.normal))
        fun inset(axis: Vec3, old: Int, count: Int): Double {
            val cell = floor(impact.subtract(position).dot(axis) + old / 2.0).toInt().coerceIn(0, old - 1)
            val start = ((old - count) / 2).coerceIn(maxOf(0, cell - count + 1), minOf(old - count, cell))
            return start - (old - count) / 2.0
        }
        return copy(position = position.add(u.scale(inset(u, width, w))).add(v.scale(inset(v, height, h))), width = w, height = h)
    }

    fun available(level: Level): Boolean {
        return width in 1..32 && height in 1..32 && frame(level) != null && hasBacking(level) == true
    }

    internal fun hasBacking(level: Level): Boolean? {
        if (!surface) return level.hasChunkAt(BlockPos.containing(position))
        val normal = Vec3.atLowerCornerOf(face.normal)
        val up = up(face)
        val right = up.cross(normal)
        for (x in 0 until width) for (y in 0 until height) {
            val point = position.add(right.scale(x + 0.5 - width / 2.0))
                .add(up.scale(y + 0.5 - height / 2.0)).subtract(normal.scale(0.02))
            val block = BlockPos.containing(point)
            if (!level.hasChunkAt(block) || !level.hasChunkAt(block.relative(face))) return null
            if (!supported(level, block, face)) return false
        }
        return true
    }

    fun canPlace(level: Level) = available(level) && (surface || GatemakyrClearance.open(level, this))

    fun write(buffer: FriendlyByteBuf) {
        buffer.writeDouble(position.x); buffer.writeDouble(position.y); buffer.writeDouble(position.z)
        buffer.writeEnum(face); buffer.writeLong(shipId); buffer.writeBoolean(surface)
        buffer.writeVarInt(width); buffer.writeVarInt(height)
        buffer.writeDouble(impact.x); buffer.writeDouble(impact.y); buffer.writeDouble(impact.z)
    }

    companion object {
        const val SURFACE_RANGE = 192.0

        private fun up(face: Direction) = if (face.axis == Direction.Axis.Y) Vec3(0.0, 0.0, -1.0) else Vec3(0.0, 1.0, 0.0)

        private fun supported(level: Level, block: BlockPos, face: Direction): Boolean {
            val front = block.relative(face)
            return level.hasChunkAt(block) && level.hasChunkAt(front) && level.getBlockState(block).isFaceSturdy(level, block, face) &&
                level.getBlockState(front).getCollisionShape(level, front).isEmpty
        }

        fun read(buffer: FriendlyByteBuf) = GatemakyrTarget(Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
            buffer.readEnum(Direction::class.java), buffer.readLong(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
            Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()))

        internal fun acquire(player: Player, free: Boolean, distance: Double, width: Int, height: Int, partner: GatemakyrTarget?, onPath: (GatemakyrRayPath?) -> Unit = {}): GatemakyrTarget? {
            if (!distance.isFinite() || width !in 1..32 || height !in 1..32) return null
            val maxWidth = minOf(width, partner?.width ?: width)
            val maxHeight = minOf(height, partner?.height ?: height)
            val level = player.level()
            val eye = player.eyePosition
            val end = eye.add(player.lookAngle.scale(if (free) distance.coerceIn(2.0, 48.0) else SURFACE_RANGE))
            val ray = ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
            val hit = GatemakyrRaycast.surfaceHit(level, ray)
            val path = (hit as? GatemakyrHit)?.gatePath
            onPath(path)
            if (free && hit.type == HitResult.Type.MISS) {
                val travels = path?.gates.orEmpty()
                val point = travels.fold(end) { pos, gate -> gate.passage.point(pos) }
                val look = travels.fold(player.lookAngle) { dir, gate -> gate.passage.direction(dir) }
                if (!level.hasChunkAt(BlockPos.containing(point)) || hit.location.distanceToSqr(point) > 0.01) return null
                val snapped = Vec3(round(point.x * 16) / 16, round(point.y * 16) / 16, round(point.z * 16) / 16)
                return GatemakyrTarget(snapped, Direction.getNearest(-look.x, -look.y, -look.z),
                    width = maxWidth, height = maxHeight).takeIf { it.canPlace(level) }
            }
            return surface(level, hit, maxWidth, maxHeight)
        }

        internal fun surface(level: Level, hit: BlockHitResult, maxWidth: Int, maxHeight: Int): GatemakyrTarget? {
            if (hit.type != HitResult.Type.BLOCK) return null
            val ship = level.getShipManagingPos(hit.blockPos)
            val normal = Vec3.atLowerCornerOf(hit.direction.normal)
            val up = up(hit.direction)
            val right = up.cross(normal)
            val rectangle = GatemakyrSurfaceFit.fit(maxWidth, maxHeight) { u, v ->
                val block = hit.blockPos.offset((right.x * u + up.x * v).toInt(), (right.y * u + up.y * v).toInt(),
                    (right.z * u + up.z * v).toInt())
                supported(level, block, hit.direction)
            } ?: return null
            val center = Vec3.atCenterOf(hit.blockPos).add(normal.scale(0.504))
                .add(right.scale(rectangle.u + (rectangle.width - 1) / 2.0))
                .add(up.scale(rectangle.v + (rectangle.height - 1) / 2.0))
            val impact = hit.location.add(normal.scale(0.004))
            return GatemakyrTarget(center, hit.direction, ship?.id ?: -1, true, rectangle.width, rectangle.height, impact)
        }
    }
}
