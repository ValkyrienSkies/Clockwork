package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3d
import org.valkyrienskies.core.api.bodies.ClientVsBody
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object LayeredTireRenderer {
    private data class Strand(val surface: Long, val anchor: Vector3d, val tread: Vector3d,
                              val born: Long, val width: Double, var touched: Long = born)
    private data class MappedVertex(val point: Vector3d, val u: Double, val v: Double)
    private class Trail {
        val strands = ArrayList<Strand>()
        var tick = Long.MIN_VALUE
    }
    private val trails = HashMap<Long, Trail>()
    private var level: ClientLevel? = null

    fun clear() {
        trails.clear()
        level = null
    }

    fun render(world: ClientLevel?, body: ClientVsBody?, tire: WheelTire, contacts: List<TireContact>,
               stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int, partialTick: Float,
               material: TextureAtlasSprite? = null) {
        if (world != null && level !== world) { trails.clear(); level = world }
        val time = world?.gameTime ?: 0L
        if (time % 40L == 0L) trails.entries.removeIf { time - it.value.tick > 40 }
        val appearance = tire.type.appearance
        val trail = body?.takeIf { appearance.webs }?.let { trails.getOrPut(it.id) { Trail() } }
        if (world != null && body != null && trail != null && trail.tick != time) {
            trail.tick = time
            val contact = WheelEffects.surfaceContact(body.id)
            if (contact != null) {
                val surface = world.shipObjectWorld.allBodies.getById(contact.surface) as? ClientVsBody
                val point = Vector3d(contact.point)
                val normal = Vector3d(contact.normal)
                surface?.renderTransform?.toWorld?.transformPosition(point)
                surface?.renderTransform?.toWorld?.transformDirection(normal)?.normalize()
                val local = body.renderTransform.toModel.transformPosition(point, Vector3d())
                val radial = sqrt(local.y * local.y + local.z * local.z)
                if (radial > .01) for (side in listOf(-.32, .32)) {
                    val radius = minOf(radial, tire.radius) - minOf(.08, tire.thickness * .35)
                    val tread = Vector3d(side * tire.width, local.y * radius / radial, local.z * radius / radial)
                    val anchor = body.renderTransform.toWorld.transformPosition(Vector3d(local).apply { x = tread.x }, Vector3d())
                        .fma(.015, normal)
                    surface?.renderTransform?.toModel?.transformPosition(anchor)
                    val existing = trail.strands.lastOrNull { it.surface == contact.surface &&
                        it.tread.distanceSquared(tread) < .012 && it.anchor.distanceSquared(anchor) < .012 }
                    if (existing != null) existing.touched = time
                    else trail.strands.add(Strand(contact.surface, anchor, tread, time, minOf(.22, tire.width * .32)))
                }
            }
            trail.strands.removeIf { strand ->
                val surface = world.shipObjectWorld.allBodies.getById(strand.surface) as? ClientVsBody
                val anchor = Vector3d(strand.anchor)
                surface?.renderTransform?.toWorld?.transformPosition(anchor)
                body.renderTransform.toModel.transformPosition(anchor)
                time - strand.touched > 30 || strand.surface != -1L && surface == null ||
                    anchor.distance(strand.tread) > minOf(1.6, tire.radius * 1.4)
            }
            while (trail.strands.size > 24) trail.strands.removeAt(0)
        }
        val atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        val inside = material ?: atlas.apply(requireNotNull(appearance.innerTexture))
        val shell = appearance.shellTexture?.let { atlas.apply(it) }
        for (outer in if (shell != null) listOf(false, true) else listOf(false)) {
            val buffer = buffers.getBuffer(if (outer) Sheets.translucentCullBlockSheet() else Sheets.solidBlockSheet())
            val sprite = if (outer) requireNotNull(shell) else inside
            ring(tire, contacts, outer || shell == null, stack, buffer, sprite, light, overlay)
            if (world != null && body != null && trail != null) for (strand in trail.strands) {
                val anchor = Vector3d(strand.anchor)
                if (strand.surface != -1L) {
                    val surface = world.shipObjectWorld.allBodies.getById(strand.surface) as? ClientVsBody ?: continue
                    surface.renderTransform.toWorld.transformPosition(anchor)
                }
                body.renderTransform.toModel.transformPosition(anchor)
                val length = anchor.distance(strand.tread)
                val maximum = minOf(1.6, tire.radius * 1.4)
                if (length > maximum || length < .008) continue
                val age = time - strand.touched + partialTick
                val growth = ((time - strand.born + partialTick + 1) / 3.0).coerceAtMost(1.0)
                val fade = growth * minOf(1.0, (30 - age) / 8.0, (maximum - length) / .25).coerceAtLeast(0.0)
                web(strand.tread, anchor, strand.width * fade, outer || shell == null, stack, buffer, sprite, light, overlay)
            }
        }
    }

    fun cleats(tire: WheelTire, stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int,
               contacts: List<TireContact> = emptyList()) {
        val kind = tire.type.appearance.cleats
        if (kind == TireType.Cleats.NONE) return
        val texture = if (kind == TireType.Cleats.TEETH) "iron_block" else "black_concrete"
        val sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(ResourceLocation("minecraft", "block/$texture"))
        val buffer = buffers.getBuffer(Sheets.solidBlockSheet())
        val height = cleatHeight(tire)
        val planes = contacts.map { contact ->
            val support = (0 until 8).maxOf { i ->
                val angle = (i + .5) * PI / 4
                -tire.radius * (cos(angle) * contact.normal.y + sin(angle) * contact.normal.z)
            }
            contact.normal to (support - contact.compression.coerceIn(0f,
                (tire.thickness * tire.type.physics.compression).toFloat()))
        }
        for (segment in 0 until 8) {
            val angle = (segment + .5) * PI / 4
            val radial = Vector3d(0.0, cos(angle), sin(angle))
            val tangent = Vector3d(0.0, -sin(angle), cos(angle))
            val span = if (kind == TireType.Cleats.LUGS) tire.width * .32 else tire.width * .48
            val halfDepth = if (kind == TireType.Cleats.TEETH) .08 else .045
            fun vertex(x: Double, r: Double, z: Double): Vector3d {
                val point = Vector3d(x, 0.0, 0.0).fma(r, radial).fma(z, tangent)
                val rubber = ((r - tire.rimRadius) / tire.thickness).coerceIn(0.0, 1.0)
                for ((normal, limit) in planes) {
                    val depth = -limit - point.dot(normal)
                    if (depth > 0) point.fma(depth * rubber, normal)
                }
                return point
            }
            val points = (0..7).map { i -> vertex(if (i and 1 == 0) -span else span,
                if (i and 2 == 0) tire.radius - height else tire.radius,
                if (i and 4 == 0) -halfDepth else halfDepth) }
            for (face in arrayOf(intArrayOf(0, 4, 6, 2), intArrayOf(1, 3, 7, 5), intArrayOf(0, 1, 5, 4),
                intArrayOf(2, 6, 7, 3), intArrayOf(0, 2, 3, 1), intArrayOf(4, 5, 7, 6)))
                quad(points[face[0]], points[face[1]], points[face[2]], points[face[3]], stack, buffer, sprite, light, overlay)
        }
    }

    private fun cleatHeight(tire: WheelTire) = minOf(tire.thickness * .35, when (tire.type.appearance.cleats) {
        TireType.Cleats.PADDLES -> .18
        TireType.Cleats.TEETH -> .12
        TireType.Cleats.LUGS -> .045
        else -> 0.0
    })

    private fun ring(tire: WheelTire, contacts: List<TireContact>, outer: Boolean, stack: PoseStack,
                     buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        val inset = if (outer) 0.0 else minOf(.0625, tire.thickness * .2, tire.width * .12)
        val radius = tire.radius - inset - cleatHeight(tire)
        val width = tire.width - inset * 2
        val profile = listOf(-tire.rimWidth / 2 to tire.rimRadius, -width / 2 to radius * .90,
            -width * .34 to radius, width * .34 to radius, width / 2 to radius * .90,
            tire.rimWidth / 2 to tire.rimRadius)
        val planes = contacts.map { contact ->
            val support = (0 until 8).maxOf { i ->
                -radius * (cos(i * PI / 4) * contact.normal.y + sin(i * PI / 4) * contact.normal.z)
            }
            contact.normal to (support - contact.compression.coerceAtMost((tire.thickness * tire.type.physics.compression).toFloat()))
        }
        fun vertex(x: Double, r: Double, angle: Double): Vector3d {
            val p = Vector3d(x, cos(angle) * r, sin(angle) * r)
            val rubber = ((r - tire.rimRadius) / tire.thickness).coerceIn(0.0, 1.0)
            for ((normal, limit) in planes) {
                val depth = -limit - p.dot(normal)
                if (depth > 0) p.fma(depth * rubber, normal)
            }
            return p
        }
        for (band in 0 until profile.lastIndex) for (segment in 0 until 8) {
            val a = profile[band]; val b = profile[band + 1]
            val angle = segment * PI / 4; val next = (segment + 1) * PI / 4
            quad(vertex(a.first, a.second, angle), vertex(a.first, a.second, next),
                vertex(b.first, b.second, next), vertex(b.first, b.second, angle), stack, buffer, sprite, light, overlay)
        }
    }

    private fun web(start: Vector3d, end: Vector3d, width: Double, outer: Boolean, stack: PoseStack,
                    buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        if (width < .001) return
        val direction = Vector3d(end).sub(start).normalize()
        val across = direction.cross(if (abs(direction.x) < .8) Vector3d(1.0, 0.0, 0.0)
            else Vector3d(0.0, 1.0, 0.0), Vector3d()).normalize()
        val side = direction.cross(across, Vector3d()).normalize()
        val rings = (0..4).map { i ->
            val t = i / 4.0
            val center = Vector3d(start).lerp(end, t)
            val neck = .16 + .84 * (2 * t - 1) * (2 * t - 1)
            val radius = width * neck * if (outer) 1.0 else .65
            (0 until 4).map { corner -> Vector3d(center)
                .fma(if (corner == 0 || corner == 3) -radius else radius, across)
                .fma(if (corner < 2) -radius else radius, side) }
        }
        for (i in 0..3) for (j in 0..3) {
            val next = (j + 1) % 4
            quad(rings[i][j], rings[i][next], rings[i + 1][next], rings[i + 1][j], stack, buffer, sprite, light, overlay)
        }
        quad(rings[0][3], rings[0][2], rings[0][1], rings[0][0], stack, buffer, sprite, light, overlay)
        quad(rings[4][0], rings[4][1], rings[4][2], rings[4][3], stack, buffer, sprite, light, overlay)
    }

    private fun quad(a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d, stack: PoseStack,
                     buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        if (a.distanceSquared(b) < 1e-12 || a.distanceSquared(d) < 1e-12) return
        val normal = Vector3d(b).sub(a).cross(Vector3d(d).sub(a))
        if (normal.lengthSquared() < 1e-12) return
        normal.normalize()
        val uAxis = Vector3d(b).sub(a).normalize()
        val vAxis = normal.cross(uAxis, Vector3d())
        val projected = listOf(a, b, c, d).map { point ->
            val relative = Vector3d(point).sub(a)
            MappedVertex(point, relative.dot(uAxis), relative.dot(vAxis))
        }
        val minU = projected.minOf { it.u }; val minV = projected.minOf { it.v }
        val vertices = projected.map { it.copy(u = it.u - minU, v = it.v - minV) }
        val width = vertices.maxOf { it.u }; val height = vertices.maxOf { it.v }
        val pose = stack.last()
        fun emit(vertex: MappedVertex, x: Int, y: Int) {
            val point = vertex.point
            buffer.vertex(pose.pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat()).color(1f, 1f, 1f, 1f)
                .uv(sprite.getU(((vertex.u - x) * 16).coerceIn(0.0, 16.0)),
                    sprite.getV(((vertex.v - y) * 16).coerceIn(0.0, 16.0)))
                .overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
        }
        if (width <= 1 && height <= 1) {
            vertices.forEach { emit(it, 0, 0) }
            return
        }
        fun clip(input: List<MappedVertex>, axis: Int, edge: Double, lower: Boolean): List<MappedVertex> {
            if (input.isEmpty()) return input
            val output = ArrayList<MappedVertex>(6)
            fun distance(vertex: MappedVertex) = ((if (axis == 0) vertex.u else vertex.v) - edge) * if (lower) 1 else -1
            var previous = input.last(); var previousDistance = distance(previous)
            for (vertex in input) {
                val currentDistance = distance(vertex)
                if ((currentDistance >= 0) != (previousDistance >= 0)) {
                    val t = previousDistance / (previousDistance - currentDistance)
                    output.add(MappedVertex(Vector3d(previous.point).lerp(vertex.point, t),
                        previous.u + (vertex.u - previous.u) * t, previous.v + (vertex.v - previous.v) * t))
                }
                if (currentDistance >= 0) output.add(vertex)
                previous = vertex; previousDistance = currentDistance
            }
            return output
        }
        for (x in 0 until ceil(width).toInt()) for (y in 0 until ceil(height).toInt()) {
            val polygon = clip(clip(clip(clip(vertices, 0, x.toDouble(), true), 0, x + 1.0, false),
                1, y.toDouble(), true), 1, y + 1.0, false)
            if (polygon.size == 4) polygon.forEach { emit(it, x, y) }
            else for (i in 1 until polygon.lastIndex) {
                emit(polygon[0], x, y); emit(polygon[i], x, y)
                emit(polygon[i + 1], x, y); emit(polygon[i + 1], x, y)
            }
        }
    }
}
