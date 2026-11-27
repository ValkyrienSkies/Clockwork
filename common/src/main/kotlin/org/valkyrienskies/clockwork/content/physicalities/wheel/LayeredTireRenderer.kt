package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import org.valkyrienskies.core.api.bodies.ClientVsBody
import org.valkyrienskies.core.api.bodies.properties.BodyTransform
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

internal object LayeredTireRenderer {
    private const val SEGMENTS = 8
    private const val MAX_STRANDS = 24
    private val BOX_FACES = arrayOf(intArrayOf(0, 4, 6, 2), intArrayOf(1, 3, 7, 5), intArrayOf(0, 1, 5, 4),
        intArrayOf(2, 6, 7, 3), intArrayOf(0, 2, 3, 1), intArrayOf(4, 5, 7, 6))
    private val RING_DIRECTIONS = directions(-.5)
    private val CLEAT_DIRECTIONS = directions(0.0)
    private val LUG_DIRECTIONS = directions(.5)

    private data class Strand(val surface: Long, val anchor: Vector3d, val site: Int,
                              val born: Long, val width: Double, var touched: Long = born)
    private data class Glob(val point: Vector3d, var width: Double)
    private class Trail {
        val strands = ArrayList<Strand>()
        var tick = Long.MIN_VALUE
        var tire: WheelTire? = null
    }
    private class Geometry(tire: WheelTire) {
        val cleats by lazy(LazyThreadSafetyMode.NONE) { cleatGeometry(tire) }
        val globs by lazy(LazyThreadSafetyMode.NONE) { globGeometry(tire) }
        val innerRing by lazy(LazyThreadSafetyMode.NONE) { ringGeometry(tire, false) }
        val outerRing by lazy(LazyThreadSafetyMode.NONE) { ringGeometry(tire, true) }
    }
    private class CleatGeometry(val points: Array<Vector3d>, val rubber: DoubleArray, val directions: Array<Vector3d>)
    private class RingGeometry(val points: Array<Vector3d>, val rubber: DoubleArray, val radius: Double)
    private class WebMesh {
        val inner = Array(20) { Vector3d() }
        val outer = Array(20) { Vector3d() }
    }
    private class MappedVertex {
        val point = Vector3d()
        var u = 0.0
        var v = 0.0
    }
    private class Polygon {
        private val vertices = ArrayList<MappedVertex>(12).apply { repeat(12) { add(MappedVertex()) } }
        var size = 0
        operator fun get(index: Int) = vertices[index]
        fun append(): MappedVertex {
            if (size == vertices.size) vertices.add(MappedVertex())
            return vertices[size++]
        }
    }

    private val trails = HashMap<Long, Trail>()
    private var level: ClientLevel? = null
    private var lastCleanup = Long.MIN_VALUE
    private val geometryCache = LinkedHashMap<WheelTire, Geometry>()
    private val treadSprites = HashMap<ResourceLocation, TextureAtlasSprite>()
    private var treadAtlasMarker: TextureAtlasSprite? = null

    private val boxPoints = Array(8) { Vector3d() }
    private val globPoints = Array(24) { Glob(Vector3d(), 0.0) }
    private val ringPoints = Array(6 * (SEGMENTS + 1)) { Vector3d() }
    private val webMeshes = Array(MAX_STRANDS) { WebMesh() }
    private val radial = Vector3d()
    private val tangent = Vector3d()
    private val webAnchor = Vector3d()
    private val webDirection = Vector3d()
    private val webAcross = Vector3d()
    private val webSide = Vector3d()
    private val webCenter = Vector3d()
    private val quadNormal = Vector3d()
    private val quadU = Vector3d()
    private val quadV = Vector3d()
    private val quadRelative = Vector3d()
    private val projected = Polygon()
    private val tileScratch = Polygon()
    private val tilePolygon = Polygon()
    private val columnScratch = Polygon()
    private val columnPolygon = Polygon()
    private val wearScratch = Polygon()
    private val wearPolygon = Polygon()
    private val vertexPosition = Vector4f()
    private val vertexNormal = Vector3f()

    fun clear() {
        trails.clear()
        level = null
        lastCleanup = Long.MIN_VALUE
    }

    private fun geometry(tire: WheelTire): Geometry = geometryCache.getOrPut(tire) {
        if (geometryCache.size >= 128) geometryCache.remove(geometryCache.keys.first())
        Geometry(tire)
    }

    private fun directions(phase: Double) = Array(SEGMENTS + 1) { segment ->
        val angle = (segment + phase) * PI / 4
        Vector3d(0.0, cos(angle), sin(angle))
    }

    private fun support(directions: Array<Vector3d>, radius: Double, normal: Vector3d): Double =
        (0 until SEGMENTS).maxOf { i -> -radius * (directions[i].y * normal.y + directions[i].z * normal.z) }

    private fun surfaceTransform(world: ClientLevel, id: Long): BodyTransform? =
        (world.shipObjectWorld.allShips.getById(id) as? ClientShip)?.renderTransform
            ?: (world.shipObjectWorld.allBodies.getById(id) as? ClientVsBody)?.renderTransform

    fun render(world: ClientLevel?, body: ClientVsBody?, tire: WheelTire, contacts: List<TireContact>,
               stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int, partialTick: Float,
               material: TextureAtlasSprite? = null) {
        if (world != null && level !== world) { clear(); level = world }
        val time = world?.gameTime ?: 0L
        if (world != null && time != lastCleanup && time % 40L == 0L) {
            lastCleanup = time
            trails.entries.removeIf { time - it.value.tick > 40 }
        }
        val appearance = tire.type.appearance
        val geometry = geometry(tire)
        if (appearance.webs) deformGlobs(tire, contacts, geometry.globs)
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
                for (site in globPoints.indices) {
                    val glob = globPoints[site]
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
                    anchor.distance(globPoints[strand.site].point) > minOf(1.6, tire.radius * 1.4)
            }
            while (trail.strands.size > MAX_STRANDS) trail.strands.removeAt(0)
        }
        val atlas = Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
        val inside = material ?: atlas.getSprite(requireNotNull(appearance.innerTexture))
        val shell = appearance.shellTexture?.let { atlas.getSprite(it) }
        var webCount = 0
        if (world != null && body != null && trail != null && trail.strands.isNotEmpty()) {
            val maximum = minOf(1.6, tire.radius * 1.4)
            val toModel = body.renderTransform.toModel
            for (strand in trail.strands) {
                val anchor = webAnchor.set(strand.anchor)
                if (strand.surface != -1L) {
                    val surface = surfaceTransform(world, strand.surface) ?: continue
                    surface.toWorld.transformPosition(anchor)
                }
                toModel.transformPosition(anchor)
                val tread = globPoints[strand.site].point
                val length = anchor.distance(tread)
                if (length > maximum || length < .008) continue
                val age = time - strand.touched + partialTick
                val growth = ((time - strand.born + partialTick + 1) / 3.0).coerceAtMost(1.0)
                val fade = growth * minOf(1.0, (30 - age) / 8.0, (maximum - length) / .25).coerceAtLeast(0.0)
                val width = strand.width * fade
                if (width < .001) continue
                buildWeb(webMeshes[webCount++], tread, anchor, width, shell != null)
            }
        }
        for (layer in 0 until if (shell == null) 1 else 2) {
            val outer = layer == 1 || shell == null
            val buffer = buffers.getBuffer(if (layer == 1) Sheets.translucentCullBlockSheet() else Sheets.solidBlockSheet())
            val sprite = if (layer == 1) requireNotNull(shell) else inside
            ring(tire, contacts, if (outer) geometry.outerRing else geometry.innerRing, stack, buffer, sprite, light, overlay)
            if (appearance.webs) for (glob in globPoints) renderGlob(glob, outer, stack, buffer, sprite, light, overlay)
            for (i in 0 until webCount) {
                val mesh = webMeshes[i]
                renderWeb(if (outer) mesh.outer else mesh.inner, stack, buffer, sprite, light, overlay)
            }
        }
    }

    private fun globGeometry(tire: WheelTire): Array<Glob> = Array(24) { site ->
        val row = site % 2
        val angle = (site / 2 + row * .43) * PI / 6
        val faceAngle = round(angle / (PI / 4)) * PI / 4
        val radius = tire.carcassRadius * cos(PI / 8) / cos(angle - faceAngle)
        val point = Vector3d((if (row == 0) -.24 else .24) * tire.width, cos(angle) * radius, sin(angle) * radius)
        Glob(point, minOf(.14, tire.width * .23) * (.75 + .25 * sin(site * 2.4)))
    }

    private fun deformGlobs(tire: WheelTire, contacts: List<TireContact>, base: Array<Glob>) {
        val deformation = TireDeformation(tire, contacts) { support(RING_DIRECTIONS, tire.carcassRadius, it) }
        for (i in base.indices) {
            val glob = globPoints[i]
            deformation.apply(glob.point.set(base[i].point), 1.0)
            glob.width = base[i].width
        }
    }

    private fun renderGlob(glob: Glob, outer: Boolean, stack: PoseStack, buffer: VertexConsumer,
                           sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        radial.set(0.0, glob.point.y, glob.point.z).normalize()
        tangent.set(0.0, radial.z, -radial.y)
        val width = glob.width * if (outer) 1.0 else .65
        for (i in boxPoints.indices) boxPoints[i].set(glob.point)
            .add(if (i and 1 == 0) -width else width, 0.0, 0.0)
            .fma(if (i and 2 == 0) -width else width, tangent)
            .fma(if (i and 4 == 0) -width * .45 else width * .7, radial)
        for (face in BOX_FACES)
            quad(boxPoints[face[0]], boxPoints[face[1]], boxPoints[face[2]], boxPoints[face[3]], stack, buffer, sprite, light, overlay)
    }

    private fun treadSprite(texture: ResourceLocation): TextureAtlasSprite {
        val atlas = Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)
        val marker = atlas.getSprite(MissingTextureAtlasSprite.getLocation())
        if (treadAtlasMarker !== marker) {
            treadAtlasMarker = marker
            treadSprites.clear()
        }
        return treadSprites.getOrPut(texture) {
            atlas.getSprite(ResourceLocation(texture.namespace, texture.path.removePrefix("textures/").removeSuffix(".png")))
        }
    }

    fun cleats(tire: WheelTire, stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int,
               contacts: List<TireContact> = emptyList()) {
        val kind = tire.type.appearance.cleats
        if (kind == TireType.Cleats.NONE) return
        if (kind == TireType.Cleats.LUGS && tire.wearGrade == 10) return
        val sprite = treadSprite(tire.type.appearance.treadTexture)
        val buffer = buffers.getBuffer(Sheets.solidBlockSheet())
        val base = geometry(tire).cleats
        val deformation = TireDeformation(tire, contacts) { support(base.directions, tire.carcassRadius, it) }
        for (segment in 0 until SEGMENTS) {
            for (i in boxPoints.indices) deformation.apply(boxPoints[i].set(base.points[segment * 8 + i]), base.rubber[i])
            for (face in BOX_FACES)
                quad(boxPoints[face[0]], boxPoints[face[1]], boxPoints[face[2]], boxPoints[face[3]], stack, buffer, sprite, light, overlay, 4.0, 8.0, kind == TireType.Cleats.PADDLES)
        }
    }

    private fun cleatGeometry(tire: WheelTire): CleatGeometry {
        val kind = tire.type.appearance.cleats
        val height = cleatHeight(tire)
        val directions = if (kind == TireType.Cleats.LUGS) LUG_DIRECTIONS else CLEAT_DIRECTIONS
        val span = if (kind == TireType.Cleats.LUGS) tire.width * .32 else tire.width * .48
        val halfDepth = if (kind == TireType.Cleats.TEETH) .08 else .045
        val root = if (kind == TireType.Cleats.LUGS) tire.carcassRadius - height
            else (tire.carcassRadius - height) * cos(PI / 8) - .002
        val tip = tire.carcassRadius - (tire.carcassRadius - root) * tire.wearGrade *
            (if (kind == TireType.Cleats.PADDLES) .065 else .1)
        val points = Array(SEGMENTS * 8) { Vector3d() }
        val tangent = Vector3d()
        for (segment in 0 until SEGMENTS) {
            val radial = directions[segment]
            tangent.set(0.0, -radial.z, radial.y)
            for (i in 0 until 8) points[segment * 8 + i].set(if (i and 1 == 0) -span else span, 0.0, 0.0)
                .fma(if (i and 2 == 0) root else tip, radial).fma(if (i and 4 == 0) -halfDepth else halfDepth, tangent)
        }
        return CleatGeometry(points, DoubleArray(8) { rubber(tire, if (it and 2 == 0) root else tip) }, directions)
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

    private fun rubber(tire: WheelTire, radius: Double) =
        ((radius - tire.rimRadius) / tire.carcassThickness).coerceIn(0.0, 1.0)

    private fun ringGeometry(tire: WheelTire, outer: Boolean): RingGeometry {
        val inset = if (outer) 0.0 else minOf(.0625, tire.carcassThickness * .2, tire.width * .12)
        val radius = tire.carcassRadius - inset - cleatHeight(tire)
        val width = tire.width - inset * 2
        val profile = profile(tire, width, radius)
        val points = Array(profile.size * (SEGMENTS + 1)) { i ->
            val (x, r) = profile[i / (SEGMENTS + 1)]
            val direction = RING_DIRECTIONS[i % (SEGMENTS + 1)]
            Vector3d(x, direction.y * r, direction.z * r)
        }
        return RingGeometry(points, DoubleArray(profile.size) { rubber(tire, profile[it].second) }, radius)
    }

    private fun ring(tire: WheelTire, contacts: List<TireContact>, base: RingGeometry, stack: PoseStack,
                     buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int) {
        val deformation = TireDeformation(tire, contacts) { support(RING_DIRECTIONS, base.radius, it) }
        for (i in base.points.indices) deformation.apply(ringPoints[i].set(base.points[i]), base.rubber[i / (SEGMENTS + 1)])
        for (band in 0 until base.rubber.lastIndex) for (segment in 0 until SEGMENTS) {
            val a = band * (SEGMENTS + 1) + segment
            val b = a + SEGMENTS + 1
            quad(ringPoints[a], ringPoints[a + 1], ringPoints[b + 1], ringPoints[b], stack, buffer, sprite, light, overlay,
                wearGrade = if (band == 2) tire.wearGrade else 0, segment = segment)
        }
    }

    private fun buildWeb(mesh: WebMesh, start: Vector3d, end: Vector3d, width: Double, inner: Boolean) {
        webDirection.set(end).sub(start).normalize()
        if (abs(webDirection.x) < .8) webAcross.set(1.0, 0.0, 0.0) else webAcross.set(0.0, 1.0, 0.0)
        webDirection.cross(webAcross, webAcross).normalize()
        webDirection.cross(webAcross, webSide).normalize()
        for (i in 0..4) {
            val t = i / 4.0
            webCenter.set(start).lerp(end, t)
            val neck = .16 + .84 * (2 * t - 1) * (2 * t - 1)
            val radius = width * neck
            webRing(mesh.outer, i * 4, radius)
            if (inner) webRing(mesh.inner, i * 4, radius * .65)
        }
    }

    private fun webRing(points: Array<Vector3d>, offset: Int, radius: Double) {
        for (corner in 0 until 4) points[offset + corner].set(webCenter)
            .fma(if (corner == 0 || corner == 3) -radius else radius, webAcross)
            .fma(if (corner < 2) -radius else radius, webSide)
    }

    private fun renderWeb(points: Array<Vector3d>, stack: PoseStack, buffer: VertexConsumer, sprite: TextureAtlasSprite,
                          light: Int, overlay: Int) {
        for (i in 0..3) for (j in 0..3) {
            val next = (j + 1) % 4
            quad(points[4 * i + j], points[4 * i + next], points[4 * (i + 1) + next], points[4 * (i + 1) + j],
                stack, buffer, sprite, light, overlay)
        }
        quad(points[3], points[2], points[1], points[0], stack, buffer, sprite, light, overlay)
        quad(points[16], points[17], points[18], points[19], stack, buffer, sprite, light, overlay)
    }

    private fun project(vertex: MappedVertex, point: Vector3d, origin: Vector3d, u: Vector3d, v: Vector3d) {
        vertex.point.set(point)
        quadRelative.set(point).sub(origin)
        vertex.u = quadRelative.dot(u)
        vertex.v = quadRelative.dot(v)
    }

    private fun clip(input: Polygon, output: Polygon, axis: Int, edge: Double, lower: Boolean): Polygon {
        output.size = 0
        if (input.size == 0) return output
        val sign = if (lower) 1 else -1
        var previous = input[input.size - 1]
        var previousDistance = ((if (axis == 0) previous.u else previous.v) - edge) * sign
        for (i in 0 until input.size) {
            val current = input[i]
            val distance = ((if (axis == 0) current.u else current.v) - edge) * sign
            if ((distance >= 0) != (previousDistance >= 0)) {
                val t = previousDistance / (previousDistance - distance)
                val vertex = output.append()
                vertex.point.set(previous.point).lerp(current.point, t)
                vertex.u = previous.u + (current.u - previous.u) * t
                vertex.v = previous.v + (current.v - previous.v) * t
            }
            if (distance >= 0) {
                val vertex = output.append()
                vertex.point.set(current.point)
                vertex.u = current.u
                vertex.v = current.v
            }
            previous = current
            previousDistance = distance
        }
        return output
    }

    private fun quad(a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d, stack: PoseStack,
                     buffer: VertexConsumer, sprite: TextureAtlasSprite, light: Int, overlay: Int,
                     uScale: Double = 16.0, vScale: Double = 16.0, paddle: Boolean = false,
                     wearGrade: Int = 0, segment: Int = 0) {
        if (a.distanceSquared(b) < 1e-12 || a.distanceSquared(d) < 1e-12) return
        quadU.set(b).sub(a)
        quadNormal.set(quadU).cross(quadRelative.set(d).sub(a))
        if (quadNormal.lengthSquared() < 1e-12) return
        quadNormal.normalize()
        quadU.normalize()
        quadNormal.cross(quadU, quadV)
        val swap = paddle && abs(quadU.x) > .5
        val u = if (swap) quadV else quadU
        val v = if (swap) quadU else quadV
        project(projected[0], a, a, u, v)
        project(projected[1], b, a, u, v)
        project(projected[2], c, a, u, v)
        project(projected[3], d, a, u, v)
        projected.size = 4
        var minU = projected[0].u; var minV = projected[0].v
        for (i in 1..3) {
            minU = minOf(minU, projected[i].u)
            minV = minOf(minV, projected[i].v)
        }
        var maxU = Double.NEGATIVE_INFINITY; var maxV = Double.NEGATIVE_INFINITY
        for (i in 0..3) {
            val vertex = projected[i]
            vertex.u -= minU; vertex.v -= minV
            maxU = maxOf(maxU, vertex.u); maxV = maxOf(maxV, vertex.v)
        }
        val width = maxU; val height = maxV
        val tileWidth = if (paddle) 2.0 / 16 else 1.0
        val tileHeight = if (paddle) 10.0 / 16 else 1.0
        val uOffset = if (paddle) 3.0 / 4 else 0.0
        val vOffset = if (paddle) 11.0 / 2 else 0.0
        val pose = stack.last()
        val matrix = pose.pose()
        pose.normal().transform(vertexNormal.set(quadNormal.x.toFloat(), quadNormal.y.toFloat(), quadNormal.z.toFloat()))
        val nx = vertexNormal.x; val ny = vertexNormal.y; val nz = vertexNormal.z
        fun emit(vertex: MappedVertex, x: Int, y: Int, polished: Boolean) {
            val point = vertex.point
            matrix.transform(vertexPosition.set(point.x.toFloat(), point.y.toFloat(), point.z.toFloat(), 1f))
            buffer.vertex(vertexPosition.x, vertexPosition.y, vertexPosition.z, 1f, 1f, 1f, 1f,
                sprite.getU(if (polished) 7.25 + 1.5 * (vertex.u - x) else uOffset + ((vertex.u - x * tileWidth) * uScale).coerceIn(0.0, tileWidth * uScale)),
                sprite.getV(if (polished) 8.5 else vOffset + ((vertex.v - y * tileHeight) * vScale).coerceIn(0.0, tileHeight * vScale)),
                overlay, light, nx, ny, nz)
        }
        fun draw(points: Polygon, x: Int, y: Int, polished: Boolean = false) {
            if (points.size == 4) for (i in 0..3) emit(points[i], x, y, polished)
            else for (i in 1 until points.size - 1) {
                emit(points[0], x, y, polished); emit(points[i], x, y, polished)
                emit(points[i + 1], x, y, polished); emit(points[i + 1], x, y, polished)
            }
        }
        if (wearGrade == 0 && width <= tileWidth && height <= tileHeight) {
            draw(projected, 0, 0)
            return
        }
        for (x in 0 until ceil(width / tileWidth).toInt()) for (y in 0 until ceil(height / tileHeight).toInt()) {
            clip(projected, tileScratch, 0, x * tileWidth, true)
            clip(tileScratch, tilePolygon, 0, (x + 1) * tileWidth, false)
            clip(tilePolygon, tileScratch, 1, y * tileHeight, true)
            val polygon = clip(tileScratch, tilePolygon, 1, (y + 1) * tileHeight, false)
            if (wearGrade == 0) draw(polygon, x, y)
            else for (part in 0 until 4) {
                clip(polygon, columnScratch, 0, width * part / 4, true)
                val column = clip(columnScratch, columnPolygon, 0, width * (part + 1) / 4, false)
                if (column.size < 3) continue
                val half = (WheelVisuals.wornHalfWidth(wearGrade, segment, part) / .68).coerceAtMost(.5)
                if (half <= 0) draw(column, x, y)
                else {
                    val left = height * (.5 - half)
                    val right = height * (.5 + half)
                    draw(clip(column, wearScratch, 1, left, false), x, y)
                    clip(column, wearScratch, 1, left, true)
                    draw(clip(wearScratch, wearPolygon, 1, right, false), x, y, true)
                    draw(clip(column, wearScratch, 1, right, true), x, y)
                }
            }
        }
    }
}
