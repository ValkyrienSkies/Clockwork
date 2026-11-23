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
import org.valkyrienskies.core.api.bodies.properties.BodyTransform
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.round

internal object LayeredTireRenderer {
    private data class Strand(val surface: Long, val anchor: Vector3d, val site: Int,
                              val born: Long, val width: Double, var touched: Long = born)
    private data class Glob(val point: Vector3d, val width: Double)
    private data class MappedVertex(val point: Vector3d, val u: Double, val v: Double)
    private class Trail {
        val strands = ArrayList<Strand>()
        var tick = Long.MIN_VALUE
        var tire: WheelTire? = null
    }
    private val trails = HashMap<Long, Trail>()
    private var level: ClientLevel? = null

    fun clear() {
        trails.clear()
        level = null
    }

    private fun surfaceTransform(world: ClientLevel, id: Long): BodyTransform? =
        (world.shipObjectWorld.allShips.getById(id) as? ClientShip)?.renderTransform
            ?: (world.shipObjectWorld.allBodies.getById(id) as? ClientVsBody)?.renderTransform

    fun render(world: ClientLevel?, body: ClientVsBody?, tire: WheelTire, contacts: List<TireContact>,
               stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int, partialTick: Float,
               material: TextureAtlasSprite? = null) {
        if (world != null && level !== world) { trails.clear(); level = world }
        val time = world?.gameTime ?: 0L
        if (time % 40L == 0L) trails.entries.removeIf { time - it.value.tick > 40 }
        val appearance = tire.type.appearance
        val globs = if (appearance.webs) globs(tire, contacts) else emptyList()
        val trail = body?.takeIf { appearance.webs }?.let { trails.getOrPut(it.id) { Trail() } }?.also {
            if (it.tire != tire) { it.strands.clear(); it.tire = tire; it.tick = Long.MIN_VALUE }
        }
        if (world != null && body != null && trail != null && trail.tick != time) {
            trail.tick = time
            val contact = WheelEffects.surfaceContact(body.id)
            if (contact != null) {
                val surface = surfaceTransform(world, contact.surface)
                val point = Vector3d(contact.point)
                val normal = Vector3d(contact.normal)
                surface?.toWorld?.transformPosition(point)
                surface?.toWorld?.transformDirection(normal)?.normalize()
                val localNormal = body.renderTransform.toModel.transformDirection(normal, Vector3d()).normalize()
                for ((site, glob) in globs.withIndex()) {
                    val radial = Vector3d(0.0, glob.point.y, glob.point.z).normalize()
                    if (radial.dot(localNormal) > -.5) continue
                    val anchor = body.renderTransform.toWorld.transformPosition(glob.point, Vector3d())
                    val gap = Vector3d(anchor).sub(point).dot(normal)
                    val reach = glob.width * body.renderTransform.scaling.x() + .08
                    if (gap !in -reach..reach) continue
                    anchor.fma(.015 - gap, normal)
                    surface?.toModel?.transformPosition(anchor)
                    val existing = trail.strands.lastOrNull { it.surface == contact.surface && it.site == site &&
                        it.anchor.distanceSquared(anchor) < .012 }
                    if (existing != null) existing.touched = time
                    else trail.strands.add(Strand(contact.surface, anchor, site, time, glob.width))
                }
            }
            trail.strands.removeIf { strand ->
                val surface = surfaceTransform(world, strand.surface)
                val anchor = Vector3d(strand.anchor)
                surface?.toWorld?.transformPosition(anchor)
                body.renderTransform.toModel.transformPosition(anchor)
                time - strand.touched > 30 || strand.surface != -1L && surface == null ||
                    anchor.distance(globs[strand.site].point) > minOf(1.6, tire.radius * 1.4)
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
            for (glob in globs) renderGlob(glob, outer || shell == null, stack, buffer, sprite, light, overlay)
            if (world != null && body != null && trail != null) for (strand in trail.strands) {
                val anchor = Vector3d(strand.anchor)
                if (strand.surface != -1L) {
                    val surface = surfaceTransform(world, strand.surface) ?: continue
                    surface.toWorld.transformPosition(anchor)
                }
                body.renderTransform.toModel.transformPosition(anchor)
                val tread = globs[strand.site].point
                val length = anchor.distance(tread)
                val maximum = minOf(1.6, tire.radius * 1.4)
                if (length > maximum || length < .008) continue
                val age = time - strand.touched + partialTick
                val growth = ((time - strand.born + partialTick + 1) / 3.0).coerceAtMost(1.0)
                val fade = growth * minOf(1.0, (30 - age) / 8.0, (maximum - length) / .25).coerceAtLeast(0.0)
                web(tread, anchor, strand.width * fade, outer || shell == null, stack, buffer, sprite, light, overlay)
            }
        }
    }

    private fun globs(tire: WheelTire, contacts: List<TireContact>): List<Glob> {
        val deformation = TireDeformation(tire, contacts) { normal ->
            (0 until 8).maxOf { i ->
                val angle = (i - .5) * PI / 4
                -tire.carcassRadius * (cos(angle) * normal.y + sin(angle) * normal.z)
            }
        }
        return (0 until 24).map { site ->
            val row = site % 2
            val angle = (site / 2 + row * .43) * PI / 6
            val faceAngle = round(angle / (PI / 4)) * PI / 4
            val radius = tire.carcassRadius * cos(PI / 8) / cos(angle - faceAngle)
            val point = Vector3d((if (row == 0) -.24 else .24) * tire.width, cos(angle) * radius, sin(angle) * radius)
            deformation.apply(point, 1.0)
            Glob(point, minOf(.14, tire.width * .23) * (.75 + .25 * sin(site * 2.4)))
        }
    }

    private fun renderGlob(glob: Glob, outer: Boolean, stack: PoseStack, buffer: VertexConsumer,
                     sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        val radial = Vector3d(0.0, glob.point.y, glob.point.z).normalize()
        val tangent = Vector3d(0.0, radial.z, -radial.y)
        val width = glob.width * if (outer) 1.0 else .65
        val points = (0..7).map { i -> Vector3d(glob.point)
            .add(if (i and 1 == 0) -width else width, 0.0, 0.0)
            .fma(if (i and 2 == 0) -width else width, tangent)
            .fma(if (i and 4 == 0) -width * .45 else width * .7, radial) }
        for (face in arrayOf(intArrayOf(0, 4, 6, 2), intArrayOf(1, 3, 7, 5), intArrayOf(0, 1, 5, 4),
            intArrayOf(2, 6, 7, 3), intArrayOf(0, 2, 3, 1), intArrayOf(4, 5, 7, 6)))
            quad(points[face[0]], points[face[1]], points[face[2]], points[face[3]], stack, buffer, sprite, light, overlay)
    }

    fun cleats(tire: WheelTire, stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int,
               contacts: List<TireContact> = emptyList()) {
        val kind = tire.type.appearance.cleats
        if (kind == TireType.Cleats.NONE) return
        if (kind == TireType.Cleats.LUGS && tire.wearGrade == 10) return
        val texture = tire.type.appearance.treadTexture
        val sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(ResourceLocation(texture.namespace, texture.path.removePrefix("textures/").removeSuffix(".png")))
        val buffer = buffers.getBuffer(Sheets.solidBlockSheet())
        val height = cleatHeight(tire)
        val phase = if (kind == TireType.Cleats.LUGS) .5 else 0.0
        val deformation = TireDeformation(tire, contacts) { normal ->
            (0 until 8).maxOf { i ->
                val angle = (i + phase) * PI / 4
                -tire.carcassRadius * (cos(angle) * normal.y + sin(angle) * normal.z)
            }
        }
        for (segment in 0 until 8) {
            val angle = (segment + phase) * PI / 4
            val radial = Vector3d(0.0, cos(angle), sin(angle))
            val tangent = Vector3d(0.0, -sin(angle), cos(angle))
            val span = if (kind == TireType.Cleats.LUGS) tire.width * .32 else tire.width * .48
            val halfDepth = if (kind == TireType.Cleats.TEETH) .08 else .045
            fun vertex(x: Double, r: Double, z: Double): Vector3d {
                val point = Vector3d(x, 0.0, 0.0).fma(r, radial).fma(z, tangent)
                return deformation.apply(point, ((r - tire.rimRadius) / tire.carcassThickness).coerceIn(0.0, 1.0))
            }
            val root = if (kind == TireType.Cleats.LUGS) tire.carcassRadius - height
                else (tire.carcassRadius - height) * cos(PI / 8) - .002
            val tip = tire.carcassRadius - (tire.carcassRadius - root) * tire.wearGrade *
                (if (kind == TireType.Cleats.PADDLES) .065 else .1)
            val points = (0..7).map { i -> vertex(if (i and 1 == 0) -span else span,
                if (i and 2 == 0) root else tip,
                if (i and 4 == 0) -halfDepth else halfDepth) }
            for (face in arrayOf(intArrayOf(0, 4, 6, 2), intArrayOf(1, 3, 7, 5), intArrayOf(0, 1, 5, 4),
                intArrayOf(2, 6, 7, 3), intArrayOf(0, 2, 3, 1), intArrayOf(4, 5, 7, 6)))
                quad(points[face[0]], points[face[1]], points[face[2]], points[face[3]], stack, buffer, sprite, light, overlay, 4.0, 8.0, kind == TireType.Cleats.PADDLES)
        }
    }

    fun cleatHeight(tire: WheelTire) = minOf(tire.carcassThickness * .35, when (tire.type.appearance.cleats) {
        TireType.Cleats.PADDLES -> .18
        TireType.Cleats.TEETH -> .12
        TireType.Cleats.LUGS -> .045
        else -> 0.0
    })

    fun profile(tire: WheelTire, width: Double, radius: Double) =
        listOf(-tire.rimWidth / 2 to tire.rimRadius, -width / 2 to radius * .90,
            -width * .34 to radius, width * .34 to radius, width / 2 to radius * .90, tire.rimWidth / 2 to tire.rimRadius)

    private fun ring(tire: WheelTire, contacts: List<TireContact>, outer: Boolean, stack: PoseStack,
                     buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        val inset = if (outer) 0.0 else minOf(.0625, tire.carcassThickness * .2, tire.width * .12)
        val radius = tire.carcassRadius - inset - cleatHeight(tire)
        val width = tire.width - inset * 2
        val profile = profile(tire, width, radius)
        val deformation = TireDeformation(tire, contacts) { normal ->
            (0 until 8).maxOf { i ->
                val angle = (i - .5) * PI / 4
                -radius * (cos(angle) * normal.y + sin(angle) * normal.z)
            }
        }
        fun vertex(x: Double, r: Double, angle: Double): Vector3d {
            val p = Vector3d(x, cos(angle) * r, sin(angle) * r)
            return deformation.apply(p, ((r - tire.rimRadius) / tire.carcassThickness).coerceIn(0.0, 1.0))
        }
        for (band in 0 until profile.lastIndex) for (segment in 0 until 8) {
            val a = profile[band]; val b = profile[band + 1]
            val angle = (segment - .5) * PI / 4; val next = (segment + .5) * PI / 4
            quad(vertex(a.first, a.second, angle), vertex(a.first, a.second, next),
                vertex(b.first, b.second, next), vertex(b.first, b.second, angle), stack, buffer, sprite, light, overlay,
                wearGrade = if (band == 2) tire.wearGrade else 0, segment = segment)
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
                     buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int,
                     uScale: Double = 16.0, vScale: Double = 16.0, paddle: Boolean = false,
                     wearGrade: Int = 0, segment: Int = 0) {
        if (a.distanceSquared(b) < 1e-12 || a.distanceSquared(d) < 1e-12) return
        val normal = Vector3d(b).sub(a).cross(Vector3d(d).sub(a))
        if (normal.lengthSquared() < 1e-12) return
        normal.normalize()
        val uAxis = Vector3d(b).sub(a).normalize()
        val vAxis = normal.cross(uAxis, Vector3d())
        val swap = paddle && abs(uAxis.x) > .5
        val projected = listOf(a, b, c, d).map { point ->
            val relative = Vector3d(point).sub(a)
            MappedVertex(point, relative.dot(if (swap) vAxis else uAxis), relative.dot(if (swap) uAxis else vAxis))
        }
        val minU = projected.minOf { it.u }; val minV = projected.minOf { it.v }
        val vertices = projected.map { it.copy(u = it.u - minU, v = it.v - minV) }
        val width = vertices.maxOf { it.u }; val height = vertices.maxOf { it.v }
        val tileWidth = if (paddle) 2.0 / 16 else 1.0
        val tileHeight = if (paddle) 10.0 / 16 else 1.0
        val uOffset = if (paddle) 3.0 / 4 else 0.0
        val vOffset = if (paddle) 11.0 / 2 else 0.0
        val pose = stack.last()
        fun emit(vertex: MappedVertex, x: Int, y: Int, polished: Boolean = false) {
            val point = vertex.point
            buffer.vertex(pose.pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat()).color(1f, 1f, 1f, 1f)
                .uv(sprite.getU(if (polished) 7.25 + 1.5 * (vertex.u - x) else uOffset + ((vertex.u - x * tileWidth) * uScale).coerceIn(0.0, tileWidth * uScale)),
                    sprite.getV(if (polished) 8.5 else vOffset + ((vertex.v - y * tileHeight) * vScale).coerceIn(0.0, tileHeight * vScale)))
                .overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
        }
        if (wearGrade == 0 && width <= tileWidth && height <= tileHeight) {
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
        for (x in 0 until ceil(width / tileWidth).toInt()) for (y in 0 until ceil(height / tileHeight).toInt()) {
            val polygon = clip(clip(clip(clip(vertices, 0, x * tileWidth, true), 0, (x + 1) * tileWidth, false),
                1, y * tileHeight, true), 1, (y + 1) * tileHeight, false)
            fun draw(points: List<MappedVertex>, polished: Boolean = false) {
                if (points.size == 4) points.forEach { emit(it, x, y, polished) }
                else for (i in 1 until points.lastIndex) {
                    emit(points[0], x, y, polished); emit(points[i], x, y, polished)
                    emit(points[i + 1], x, y, polished); emit(points[i + 1], x, y, polished)
                }
            }
            if (wearGrade == 0) draw(polygon)
            else for (part in 0 until 4) {
                val column = clip(clip(polygon, 0, width * part / 4, true), 0, width * (part + 1) / 4, false)
                if (column.size < 3) continue
                val half = (WheelVisuals.wornHalfWidth(wearGrade, segment, part) / .68).coerceAtMost(.5)
                if (half <= 0) draw(column)
                else {
                    val left = height * (.5 - half)
                    val right = height * (.5 + half)
                    draw(clip(column, 1, left, false))
                    draw(clip(clip(column, 1, left, true), 1, right, false), true)
                    draw(clip(column, 1, right, true))
                }
            }
        }
    }
}
