package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.AllPartialModels
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Block
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.renderer.texture.TextureAtlas
import org.valkyrienskies.clockwork.ClockworkMod
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.multiplayer.ClientLevel
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.bodies.ClientVsBody
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.floor
import kotlin.math.ceil

class WheelHubRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<WheelHubBlockEntity>(context) {
    override fun getRotatedModel(be: WheelHubBlockEntity, state: BlockState) =
        CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, be.facing.opposite)
    override fun shouldRenderOffScreen(be: WheelHubBlockEntity) = true
    override fun getViewDistance() = Minecraft.getInstance().options.effectiveRenderDistance * 16

    override fun renderSafe(be: WheelHubBlockEntity, partialTicks: Float, ms: PoseStack,
                            buffers: MultiBufferSource, light: Int, overlay: Int) {
        super.renderSafe(be, partialTicks, ms, buffers, light, overlay)
        val wheel = be.wheel ?: return
        val tire = be.tire
        val level = be.level ?: return
        val body = level.shipObjectWorld.allBodies.getById(be.bodyId) as? ClientVsBody
        ms.pushPose()
        try {
            if (body != null) {
                val parent = level.getShipManagingPos(be.blockPos) as? ClientShip
                val relative = Matrix4d().translation(-be.blockPos.x.toDouble(), -be.blockPos.y.toDouble(), -be.blockPos.z.toDouble())
                if (parent != null) relative.mul(parent.renderTransform.worldToShip)
                relative.mul(body.renderTransform.toWorld)
                ms.mulPoseMatrix(Matrix4f(relative))
                ms.last().normal().rotate(relative.getNormalizedRotation(Quaternionf()))
            } else {
                ms.translate(.5, .5, .5)
                ms.mulPose(Quaternionf(WheelSize.axleRotation(be.facing.stepX.toDouble(), be.facing.stepY.toDouble(), be.facing.stepZ.toDouble())))
            }
            if (tire != null) {
                val coating = WheelEffects.coating(be.bodyId)
                val sprite = coating?.let { WheelEffects.coatingSprite(it.state) }
                val contacts = body?.let { WheelEffects.compression(it, partialTicks) }.orEmpty()
                val texture = WheelVisuals.treadTexture(tire.type, be.wood)
                if (texture != null) TireMesh.render(tire, contacts, ms,
                    buffers.getBuffer(RenderType.entitySolid(texture)), light, overlay)
                else LayeredTireRenderer.render(level as ClientLevel, body, tire, contacts, ms, buffers, light, overlay, partialTicks,
                    if (tire.type == TireType.WOOD) WheelVisuals.planks(be.wood) else null)
                LayeredTireRenderer.cleats(tire, ms, buffers, light, overlay, contacts)
                if (coating != null) TireMesh.render(tire, contacts, ms,
                    buffers.getBuffer(Sheets.translucentCullBlockSheet()), light, overlay, coating, sprite)
                if (be.condition.holes.isNotEmpty())
                    TirePunctureRenderer(tire, contacts).render(be.condition.holes, ms, buffers, light, overlay)
            }
            ms.pushPose()
            ms.mulPose(Quaternionf().rotationY((-PI / 2).toFloat()))
            val rim = when (wheel) {
                WheelSize.SMALL -> ClockworkPartials.WHEEL_RIM
                WheelSize.MEDIUM -> ClockworkPartials.MEDIUM_WHEEL_RIM
                WheelSize.LARGE -> ClockworkPartials.LARGE_WHEEL_RIM
            }
            ms.translate(-.5, -.5, -.5)
            CachedBuffers.partial(rim, be.blockState).light<SuperByteBuffer>(light)
                .overlay<SuperByteBuffer>(overlay).renderInto(ms,
                    WheelVisuals.rimBuffer(buffers.getBuffer(RenderType.solid()), be.tireType, be.wood))
            ms.popPose()
        } finally { ms.popPose() }
    }
}

class WheelItemRenderer : CustomRenderedItemModelRenderer() {
    override fun render(stack: ItemStack, model: CustomRenderedItemModel, renderer: PartialItemModelRenderer,
                        transformType: ItemDisplayContext, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val item = stack.item as? WheelItem ?: return
        val tire = item.size.tire(type = item.tireType ?: return).withSavedWear(stack.tag)
        ms.pushPose()
        ms.mulPose(Quaternionf().rotationY((PI / 2).toFloat()))
        val wood = WheelItem.wood(stack.tag)
        val texture = WheelVisuals.treadTexture(tire.type, wood)
        if (texture != null) TireMesh.render(tire, emptyList(), ms,
            buffers.getBuffer(RenderType.entitySolid(texture)), light, overlay)
        else LayeredTireRenderer.render(Minecraft.getInstance().level, null, tire, emptyList(), ms,
            buffers, light, overlay, 0f, if (tire.type == TireType.WOOD) WheelVisuals.planks(wood) else null)
        LayeredTireRenderer.cleats(tire, ms, buffers, light, overlay)
        ms.mulPose(Quaternionf().rotationY((-PI / 2).toFloat()))
        val rim = when (item.size) {
            WheelSize.SMALL -> ClockworkPartials.WHEEL_RIM
            WheelSize.MEDIUM -> ClockworkPartials.MEDIUM_WHEEL_RIM
            WheelSize.LARGE -> ClockworkPartials.LARGE_WHEEL_RIM
        }.get()
        if (tire.type == TireType.WOOD) {
            ms.translate(-.5, -.5, -.5)
            Minecraft.getInstance().blockRenderer.modelRenderer.renderModel(ms.last(),
                WheelVisuals.rimBuffer(buffers.getBuffer(Sheets.solidBlockSheet()), tire.type, wood),
                null, rim, 1f, 1f, 1f, light, overlay)
        } else renderer.renderSolid(rim, light)
        ms.popPose()
    }
}

internal object WheelVisuals {
    fun wornHalfWidth(grade: Int, segment: Int, part: Int): Double {
        val phase = (segment + (part + .5) / 4) * PI / 4
        return (.046 * grade - .07 + .105 * sin(3 * phase + .6) + .055 * cos(5 * phase + 1.1)).coerceIn(0.0, .46)
    }

    private val woods = setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped")
    private val woodTreads = HashMap<Block, ResourceLocation?>()
    private val rimSprite = ClockworkMod.asResource("block/wheel/wheel")

    fun treadTexture(type: TireType, wood: Block): ResourceLocation? {
        if (type.appearance.mesh != TireType.Mesh.TREAD) return null
        if (type != TireType.WOOD) return type.appearance.treadTexture
        val cached = woodTreads[wood]
        if (cached != null || woodTreads.containsKey(wood)) return cached
        val id = BuiltInRegistries.BLOCK.getKey(wood)
        val name = id.path.removeSuffix("_planks")
        val texture = if (id.namespace == "minecraft" && name in woods)
            ClockworkMod.asResource("textures/block/wheel/wooden/$name.png") else null
        woodTreads[wood] = texture
        return texture
    }

    fun planks(wood: Block): TextureAtlasSprite =
        Minecraft.getInstance().blockRenderer.blockModelShaper.getBlockModel(wood.defaultBlockState()).particleIcon

    fun rimBuffer(buffer: VertexConsumer, type: TireType?, wood: Block): VertexConsumer {
        if (type != TireType.WOOD) return buffer
        val original = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(rimSprite)
        val target = planks(wood)
        return object : VertexConsumer {
            override fun vertex(x: Double, y: Double, z: Double) = apply { buffer.vertex(x, y, z) }
            override fun color(r: Int, g: Int, b: Int, a: Int) = apply { buffer.color(r, g, b, a) }
            override fun uv(u: Float, v: Float) = apply {
                buffer.uv(target.getU(((u - original.u0) / (original.u1 - original.u0) * 16).toDouble()),
                    target.getV(((v - original.v0) / (original.v1 - original.v0) * 16).toDouble()))
            }
            override fun overlayCoords(u: Int, v: Int) = apply { buffer.overlayCoords(u, v) }
            override fun uv2(u: Int, v: Int) = apply { buffer.uv2(u, v) }
            override fun normal(x: Float, y: Float, z: Float) = apply { buffer.normal(x, y, z) }
            override fun endVertex() = buffer.endVertex()
            override fun defaultColor(r: Int, g: Int, b: Int, a: Int) = buffer.defaultColor(r, g, b, a)
            override fun unsetDefaultColor() = buffer.unsetDefaultColor()
        }
    }
}

internal class TireDeformation() {
    constructor(tire: WheelTire, contacts: List<TireContact>, compressionScale: Double = 1.0,
                support: (Vector3d) -> Double) : this() {
        reset(tire, contacts, compressionScale) { support(it) }
    }

    fun interface Support {
        fun distance(normal: Vector3d): Double
    }

    private class Plane(var normal: Vector3d, var limit: Double)
    private val planes = ArrayList<Plane>()
    private var planeCount = 0
    private var slack = 0.0
    private var flatness = 0.0
    private var carcassRadius = 0.0
    private var halfWidth = 0.0
    val isIdentity get() = planeCount == 0 && slack <= 0

    fun reset(tire: WheelTire, contacts: List<TireContact>, compressionScale: Double, support: Support) {
        slack = tire.carcassRadius - tire.radius
        flatness = (1.0 - tire.pressureKpa / 100.0).coerceAtLeast(0.0)
        carcassRadius = tire.carcassRadius
        halfWidth = tire.width / 2
        planeCount = 0
        for (i in contacts.indices) {
            val contact = contacts[i]
            val normal = contact.normal
            val meshSupport = support.distance(normal)
            val limit = if (slack > 0) minOf(meshSupport,
                tire.radius * sqrt(normal.y * normal.y + normal.z * normal.z) + halfWidth * abs(normal.x)) else meshSupport
            val compressed = limit - contact.compression.coerceIn(0f, (tire.thickness * tire.type.physics.compression * compressionScale).toFloat())
            if (planeCount == planes.size) planes.add(Plane(normal, compressed))
            else {
                val plane = planes[planeCount]
                plane.normal = normal
                plane.limit = compressed
            }
            planeCount++
        }
    }

    fun apply(point: Vector3d, rubber: Double): Vector3d {
        if (rubber <= 0.0) return point
        var fold = 0.0
        for (i in 0 until planeCount) {
            val plane = planes[i]
            val normal = plane.normal
            val limit = plane.limit
            if (slack > 0) {
                val towardContact = -(point.y * normal.y + point.z * normal.z) / carcassRadius
                fold = maxOf(fold, ((towardContact - .45) / .35).coerceIn(0.0, 1.0))
            }
            val depth = -limit - point.dot(normal)
            if (depth <= 0) continue
            point.fma(depth * (rubber + flatness * (1.0 - rubber)), normal)
        }
        if (slack > 0) {
            val bulge = minOf(halfWidth * .48, slack * (.12 + .65 * fold))
            point.x += (point.x / halfWidth).coerceIn(-1.0, 1.0) * minOf(1.0, rubber * 3) * bulge
        }
        return point
    }
}

internal object TireMesh {
    private const val SEGMENTS = 8
    private val PROFILE_V = floatArrayOf(0f, 6f, 10f, 12f, 20f, 22f, 26f, 32f)
    private const val VERTEX_STRIDE = 6
    private const val U = 3
    private const val V = 4
    private const val RUBBER = 5
    private const val SHADE = 4 * VERTEX_STRIDE
    private const val DEPOSIT = SHADE + 1
    private const val PADDLE_BAND = DEPOSIT + 1
    private const val FALLBACK_NORMAL = PADDLE_BAND + 1
    private const val QUAD_STRIDE = FALLBACK_NORMAL + 3

    private data class Geometry(var radius: Double, var width: Double, var rimRadius: Double, var rimWidth: Double,
                                var coating: Boolean, var wearGrade: Int)
    private class Mesh(val quads: FloatArray, val normals: DoubleArray, private val positions: FloatArray) : TireDeformation.Support {
        override fun distance(normal: Vector3d): Double {
            if (positions.isEmpty()) throw NoSuchElementException()
            val x = normal.x
            val y = normal.y
            val z = normal.z
            var support = Double.NEGATIVE_INFINITY
            for (i in positions.indices step 3)
                support = maxOf(support, -(positions[i] * x + positions[i + 1] * y + positions[i + 2] * z))
            return support
        }
    }

    private val lookup = Geometry(0.0, 0.0, 0.0, 0.0, false, 0)
    private val meshes = object : LinkedHashMap<Geometry, Mesh>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Geometry, Mesh>) = size > 128
    }
    private val deformation = TireDeformation()
    private val points = Array(4) { Vector3d() }
    private val normal = Vector3d()
    private val edge = Vector3d()
    private val transformedPosition = Vector4f()
    private val transformedNormal = Vector3f()

    private data class Strip(val from: Double, val to: Double, val v0: Double, val v1: Double)

    private fun strips(band: Int, pixels: Double): List<Strip> {
        val v0 = PROFILE_V[band].toDouble()
        val v1 = PROFILE_V[band + 1].toDouble()
        val borderStart = when (band) { 0 -> 2.0; 1, 5 -> 1.0; else -> 0.0 }
        val borderEnd = when (band) { 6 -> 2.0; 1, 5 -> 1.0; else -> 0.0 }
        val borderScale = if (borderStart + borderEnd > 0) minOf(1.0, pixels / (borderStart + borderEnd)) else 1.0
        val first = borderStart * borderScale
        val last = pixels - borderEnd * borderScale
        val result = ArrayList<Strip>()
        if (first > 0) result.add(Strip(0.0, first / pixels, v0 / 32, (v0 + borderStart) / 32))
        val tileStart = v0 + borderStart
        val tileSize = v1 - borderEnd - tileStart
        var position = first
        var phase = if (band == 3) ((tileSize - (last - first) % tileSize) / 2) % tileSize else 0.0
        while (position < last - 1e-8) {
            val length = minOf(tileSize - phase, last - position)
            result.add(Strip(position / pixels, (position + length) / pixels,
                (tileStart + phase) / 32, (tileStart + phase + length) / 32))
            position += length
            phase = 0.0
        }
        if (last < pixels) result.add(Strip(last / pixels, 1.0, (v1 - borderEnd) / 32, v1 / 32))
        return result
    }

    fun profile(radius: Double, width: Double, rimRadius: Double, rimWidth: Double): List<Pair<Double, Double>> {
        val sidewall = maxOf(.8, (rimRadius + (radius - rimRadius) * .25) / radius)
        return listOf(-rimWidth / (2 * width) to rimRadius / radius, -.5 to sidewall, -.38 to .97, -.26 to 1.0,
            .26 to 1.0, .38 to .97, .5 to sidewall, rimWidth / (2 * width) to rimRadius / radius)
    }

    private fun loadPoint(point: Vector3d, quads: FloatArray, vertex: Int): Vector3d =
        point.set(quads[vertex].toDouble(), quads[vertex + 1].toDouble(), quads[vertex + 2].toDouble())

    private fun faceNormal(quads: FloatArray, offset: Int) {
        normal.set(points[1]).sub(points[0]).cross(edge.set(points[3]).sub(points[0]))
        if (normal.lengthSquared() > 1e-12) normal.normalize()
        else normal.set(quads[offset + FALLBACK_NORMAL].toDouble(), quads[offset + FALLBACK_NORMAL + 1].toDouble(),
            quads[offset + FALLBACK_NORMAL + 2].toDouble())
    }

    private fun build(geometry: Geometry): Mesh {
        val (radius, width, rimRadius, rimWidth, coating, wearGrade) = geometry
        val profile = profile(radius, width, rimRadius, rimWidth)
        val positions = LinkedHashSet<Vector3f>()
        var quads = FloatArray(128 * QUAD_STRIDE)
        var normals = DoubleArray(128 * 3)
        var quadCount = 0
        for (band in 0 until profile.lastIndex) {
            val a = profile[band]
            val b = profile[band + 1]
            val nx = -(b.second - a.second) * radius * cos(PI / SEGMENTS)
            val radial = (b.first - a.first) * width
            val normalLength = sqrt(nx * nx + radial * radial)
            if (normalLength < 1e-8) continue
            val wearing = wearGrade > 0 && band in 2..4
            val sections = strips(band, normalLength * 16).flatMap { strip ->
                val density = if (wearing) 16 else if (coating) 4 else 0
                val count = ceil((strip.to - strip.from) * normalLength * density).toInt().coerceAtLeast(1)
                (0 until count).map { i -> Strip(
                    strip.from + (strip.to - strip.from) * i / count,
                    strip.from + (strip.to - strip.from) * (i + 1) / count,
                    strip.v0 + (strip.v1 - strip.v0) * i / count,
                    strip.v0 + (strip.v1 - strip.v0) * (i + 1) / count)
                }
            }
            val textureRadius = if (band == 0 || band == 6) radius * (a.second + b.second) / 2 else radius
            val repeats = (2 * SEGMENTS * sin(PI / SEGMENTS) * textureRadius).roundToInt().coerceAtLeast(1)
            val parts = if (coating) ceil(2 * sin(PI / SEGMENTS) * radius * 4).toInt().coerceAtLeast(4) else 4
            for (segment in 0 until SEGMENTS) for (part in 0 until parts) for (strip in sections) {
                val faceAngle = segment * 2 * PI / SEGMENTS
                val angle = (segment - .5) * 2 * PI / SEGMENTS
                val nextAngle = angle + 2 * PI / SEGMENTS
                val shade = if (band == 3 && segment % 2 == 0) 0.8f else 1f
                val middle = (strip.from + strip.to) * .5
                val centerX = (a.first + (b.first - a.first) * middle) * width
                val worn = wearing && abs(centerX / width) < WheelVisuals.wornHalfWidth(wearGrade, segment, part)
                val centerR = (a.second + (b.second - a.second) * middle) * radius
                val centerT = (part + .5) / parts
                val centerY = (cos(angle) * (1 - centerT) + cos(nextAngle) * centerT) * centerR
                val centerZ = (sin(angle) * (1 - centerT) + sin(nextAngle) * centerT) * centerR
                val deposit = if (coating) (.5 + (sin(centerX * 13 + centerY * 7) +
                    sin(centerZ * 11 - centerX * 5) + sin(centerY * 13 - centerZ * 7)) / 6).toFloat() else 0f
                val offset = quadCount * QUAD_STRIDE
                val n = quadCount * 3
                if (offset == quads.size) {
                    quads = quads.copyOf(quads.size * 2)
                    normals = normals.copyOf(normals.size * 2)
                }
                for (corner in 0..3) {
                    val row = if (corner < 2) strip.from else strip.to
                    val t = (if (corner == 1 || corner == 2) part + 1 else part).toDouble() / parts
                    val axial = (a.first + (b.first - a.first) * row) * width
                    val r = (a.second + (b.second - a.second) * row) * radius
                    val y = (cos(angle) * (1 - t) + cos(nextAngle) * t) * r
                    val z = (sin(angle) * (1 - t) + sin(nextAngle) * t) * r
                    val bead = (band == 0 && row == 0.0) || (band == profile.lastIndex - 1 && row == 1.0)
                    val rubber = if (bead) 0.0 else ((sqrt(y * y + z * z) - rimRadius * cos(PI / SEGMENTS)) /
                        ((radius - rimRadius) * cos(PI / SEGMENTS))).coerceIn(0.0, 1.0)
                    val vertex = offset + corner * VERTEX_STRIDE
                    quads[vertex] = axial.toFloat()
                    quads[vertex + 1] = y.toFloat()
                    quads[vertex + 2] = z.toFloat()
                    quads[vertex + U] = ((segment + t) / SEGMENTS * repeats / 4).toFloat()
                    quads[vertex + V] = if (worn) 3.5f / 32 else (if (corner < 2) strip.v0 else strip.v1).toFloat()
                    quads[vertex + RUBBER] = rubber.toFloat()
                    positions.add(Vector3f(quads[vertex], quads[vertex + 1], quads[vertex + 2]))
                    loadPoint(points[corner], quads, vertex)
                }
                val start = quads[offset + V] * 32
                val end = quads[offset + 2 * VERTEX_STRIDE + V] * 32
                quads[offset + SHADE] = shade
                quads[offset + DEPOSIT] = deposit
                quads[offset + PADDLE_BAND] = when {
                    start >= 10 && end <= 12 -> 10f / 32
                    start >= 12 && end <= 20 -> 12f / 32
                    start >= 20 && end <= 22 -> 20f / 32
                    else -> 0f
                }
                quads[offset + FALLBACK_NORMAL] = (nx / normalLength).toFloat()
                quads[offset + FALLBACK_NORMAL + 1] = (radial * cos(faceAngle) / normalLength).toFloat()
                quads[offset + FALLBACK_NORMAL + 2] = (radial * sin(faceAngle) / normalLength).toFloat()
                faceNormal(quads, offset)
                normals[n] = normal.x
                normals[n + 1] = normal.y
                normals[n + 2] = normal.z
                quadCount++
            }
        }
        val support = FloatArray(positions.size * 3)
        var p = 0
        for (position in positions) {
            support[p++] = position.x
            support[p++] = position.y
            support[p++] = position.z
        }
        return Mesh(quads.copyOf(quadCount * QUAD_STRIDE), normals.copyOf(quadCount * 3), support)
    }

    fun render(size: WheelTire, contacts: List<TireContact>, ms: PoseStack, buffer: VertexConsumer, light: Int, overlay: Int,
               coating: WheelEffects.Coating? = null, sprite: TextureAtlasSprite? = null) {
        if (coating != null && sprite == null) return
        val pose = ms.last()
        val matrix = pose.pose()
        val normalMatrix = pose.normal()
        val cleats = size.type.appearance.cleats
        val paddles = cleats == TireType.Cleats.PADDLES
        lookup.radius = size.carcassRadius - if (paddles || cleats == TireType.Cleats.TEETH) LayeredTireRenderer.cleatHeight(size) else 0.0
        lookup.width = size.width
        lookup.rimRadius = size.rimRadius
        lookup.rimWidth = size.rimWidth
        lookup.coating = coating != null
        lookup.wearGrade = if (coating == null) size.wearGrade else 0
        val mesh = meshes[lookup] ?: lookup.copy().let { key -> build(key).also { meshes[key] = it } }
        val quads = mesh.quads
        val buildup = if (coating != null) sqrt(coating.amount.coerceIn(0f, 1f)) else 0f
        deformation.reset(size, contacts, .5, mesh)
        val identity = deformation.isIdentity
        for (quad in 0 until mesh.normals.size / 3) {
            val offset = quad * QUAD_STRIDE
            val coverage = if (coating == null) 0f else
                ((.18f + buildup * .44f - quads[offset + DEPOSIT]) * 8f).coerceIn(0f, .85f) * buildup
            if (coating != null && coverage <= .01f) continue
            var moved = false
            if (!identity) for (i in 0..3) {
                val vertex = offset + i * VERTEX_STRIDE
                val p = loadPoint(points[i], quads, vertex)
                val x = p.x
                val y = p.y
                val z = p.z
                deformation.apply(p, quads[vertex + RUBBER].toDouble())
                if (!moved) moved = p.x != x || p.y != y || p.z != z
            }
            if (moved) faceNormal(quads, offset)
            else {
                val n = quad * 3
                normal.set(mesh.normals[n], mesh.normals[n + 1], mesh.normals[n + 2])
            }
            val face = normalMatrix.transform(transformedNormal.set(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()))
            val shade = quads[offset + SHADE]
            if (coating == null) {
                val paddleBand = if (paddles) quads[offset + PADDLE_BAND] else 0f
                for (i in 0..3) {
                    val vertex = offset + i * VERTEX_STRIDE
                    if (identity) transformedPosition.set(quads[vertex], quads[vertex + 1], quads[vertex + 2], 1f)
                    else {
                        val p = points[i]
                        transformedPosition.set(p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), 1f)
                    }
                    val p = matrix.transform(transformedPosition)
                    buffer.vertex(p.x, p.y, p.z, shade, shade, shade, 1f, quads[vertex + U], quads[vertex + V] - paddleBand,
                        overlay, light, face.x, face.y, face.z)
                }
            } else if (sprite != null) {
                val tile = floor(minOf(minOf(quads[offset + U], quads[offset + VERTEX_STRIDE + U]),
                    minOf(quads[offset + 2 * VERTEX_STRIDE + U], quads[offset + 3 * VERTEX_STRIDE + U])).toDouble() * 4)
                val dx = normal.x * .002
                val dy = normal.y * .002
                val dz = normal.z * .002
                val red = shade * coating.color.x
                val green = shade * coating.color.y
                val blue = shade * coating.color.z
                for (i in 0..3) {
                    val vertex = offset + i * VERTEX_STRIDE
                    val p = if (identity) loadPoint(points[i], quads, vertex) else points[i]
                    val position = matrix.transform(transformedPosition.set((p.x + dx).toFloat(), (p.y + dy).toFloat(), (p.z + dz).toFloat(), 1f))
                    val alpha = coverage * (.2f + quads[vertex + RUBBER] * .8f)
                    val u = sprite.getU(((quads[vertex + U] * 4 - tile) * 16).coerceIn(0.0, 16.0))
                    val v = sprite.getV(quads[vertex + V].toDouble() * 16)
                    buffer.vertex(position.x, position.y, position.z, red, green, blue, alpha, u, v, overlay, light, face.x, face.y, face.z)
                }
            }
        }
    }
}
