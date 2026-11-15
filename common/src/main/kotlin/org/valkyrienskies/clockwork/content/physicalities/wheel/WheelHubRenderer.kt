package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.AllPartialModels
import net.minecraft.world.level.block.state.BlockState
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
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.bodies.ClientVsBody
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
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

    override fun renderSafe(be: WheelHubBlockEntity, partialTicks: Float, ms: PoseStack,
                            buffers: MultiBufferSource, light: Int, overlay: Int) {
        super.renderSafe(be, partialTicks, ms, buffers, light, overlay)
        val size = be.tire ?: return
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
            val coating = WheelEffects.coating(be.bodyId)
            val sprite = coating?.let { WheelEffects.coatingSprite(it.state) }
            val contacts = body?.let { WheelEffects.compression(it, partialTicks) }.orEmpty()
            when (size.type.appearance.mesh) {
                TireType.Mesh.LAYERED -> LayeredTireRenderer.render(level as ClientLevel, body, size, contacts, ms, buffers, light, overlay, partialTicks)
                TireType.Mesh.TREAD -> TireMesh.render(size, contacts, ms,
                    buffers.getBuffer(RenderType.entitySolid(size.type.appearance.treadTexture)), light, overlay)
            }
            if (coating != null) TireMesh.render(size, contacts, ms,
                buffers.getBuffer(Sheets.translucentCullBlockSheet()), light, overlay, coating, sprite)
            ms.pushPose()
            ms.mulPose(Quaternionf().rotationY((-PI / 2).toFloat()))
            val rim = when (size.size) {
                WheelSize.SMALL -> ClockworkPartials.WHEEL_RIM
                WheelSize.MEDIUM -> ClockworkPartials.MEDIUM_WHEEL_RIM
                WheelSize.LARGE -> ClockworkPartials.LARGE_WHEEL_RIM
            }
            ms.translate(-.5, -.5, -.5)
            CachedBuffers.partial(rim, be.blockState).light<SuperByteBuffer>(light)
                .overlay<SuperByteBuffer>(overlay).renderInto(ms, buffers.getBuffer(RenderType.solid()))
            ms.popPose()
        } finally { ms.popPose() }
    }
}

internal object TireMesh {
    private const val SEGMENTS = 8
    private val PROFILE_V = floatArrayOf(0f, 6f, 10f, 12f, 20f, 22f, 26f, 32f)
    private data class Vertex(val x: Float, val y: Float, val z: Float, val u: Float, val v: Float,
                              val nx: Float, val ny: Float, val nz: Float, val shade: Float, val rubber: Float,
                              val deposit: Float)
    private data class Geometry(val radius: Double, val width: Double, val rimRadius: Double, val rimWidth: Double,
                                val coating: Boolean)
    private val meshes = object : LinkedHashMap<Geometry, List<Vertex>>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Geometry, List<Vertex>>) = size > 128
    }

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

    private fun build(radius: Double, width: Double, rimRadius: Double, rimWidth: Double, coating: Boolean): List<Vertex> {
        val sidewall = maxOf(.8, (rimRadius + (radius - rimRadius) * .25) / radius)
        val profile = listOf(-rimWidth / (2 * width) to rimRadius / radius, -.5 to sidewall, -.38 to .97, -.26 to 1.0,
            .26 to 1.0, .38 to .97, .5 to sidewall, rimWidth / (2 * width) to rimRadius / radius)
        val vertices = ArrayList<Vertex>()
        for (band in 0 until profile.lastIndex) {
            val a = profile[band]
            val b = profile[band + 1]
            val nx = -(b.second - a.second) * radius * cos(PI / SEGMENTS)
            val radial = (b.first - a.first) * width
            val normalLength = sqrt(nx * nx + radial * radial)
            if (normalLength < 1e-8) continue
            val sections = strips(band, normalLength * 16).flatMap { strip ->
                val count = if (coating) ceil((strip.to - strip.from) * normalLength * 4).toInt().coerceAtLeast(1) else 1
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
                val centerR = (a.second + (b.second - a.second) * middle) * radius
                val centerT = (part + .5) / parts
                val centerY = (cos(angle) * (1 - centerT) + cos(nextAngle) * centerT) * centerR
                val centerZ = (sin(angle) * (1 - centerT) + sin(nextAngle) * centerT) * centerR
                val deposit = if (coating) (.5 + (sin(centerX * 13 + centerY * 7) +
                    sin(centerZ * 11 - centerX * 5) + sin(centerY * 13 - centerZ * 7)) / 6).toFloat() else 0f
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
                    vertices.add(Vertex(axial.toFloat(), y.toFloat(), z.toFloat(),
                        ((segment + t) / SEGMENTS * repeats / 4).toFloat(), (if (corner < 2) strip.v0 else strip.v1).toFloat(),
                        (nx / normalLength).toFloat(), (radial * cos(faceAngle) / normalLength).toFloat(),
                        (radial * sin(faceAngle) / normalLength).toFloat(), shade, rubber.toFloat(), deposit))
                }
            }
        }
        return vertices
    }

    fun render(size: WheelTire, contacts: List<TireContact>, ms: PoseStack, buffer: VertexConsumer, light: Int, overlay: Int,
               coating: WheelEffects.Coating? = null, sprite: TextureAtlasSprite? = null) {
        val pose = ms.last()
        val geometry = Geometry(size.radius, size.width, size.rimRadius, size.rimWidth, coating != null)
        val vertices = meshes.getOrPut(geometry) { build(size.radius, size.width, size.rimRadius, size.rimWidth, coating != null) }
        val buildup = coating?.let { sqrt(it.amount.coerceIn(0f, 1f)) } ?: 0f
        val planes = contacts.map { contact ->
            val normal = contact.normal
            val support = vertices.maxOf { -(it.x * normal.x + it.y * normal.y + it.z * normal.z) }
            normal to (support - contact.compression.coerceIn(0f, (size.thickness * size.type.physics.compression * .5).toFloat()))
        }
        val points = Array(4) { Vector3d() }
        val normal = Vector3d()
        val edge = Vector3d()
        for (offset in vertices.indices step 4) {
            for (i in 0..3) {
                val v = vertices[offset + i]
                val p = points[i].set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
                for ((direction, limit) in planes) {
                    val penetration = -limit - p.dot(direction)
                    if (penetration > 0) p.fma(penetration * v.rubber, direction)
                }
            }
            val v0 = vertices[offset]
            normal.set(points[1]).sub(points[0]).cross(edge.set(points[3]).sub(points[0]))
            if (normal.lengthSquared() > 1e-12) normal.normalize() else normal.set(v0.nx.toDouble(), v0.ny.toDouble(), v0.nz.toDouble())
            if (coating == null) for (i in 0..3) {
                val v = vertices[offset + i]
                val p = points[i]
                buffer.vertex(pose.pose(), p.x.toFloat(), p.y.toFloat(), p.z.toFloat()).color(v.shade, v.shade, v.shade, 1f)
                    .uv(v.u, v.v).overlayCoords(overlay).uv2(light)
                    .normal(pose.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
            }
            if (coating != null && sprite != null) {
                val coverage = ((.18f + buildup * .44f - v0.deposit) * 8f).coerceIn(0f, .85f) * buildup
                if (coverage <= .01f) continue
                val tile = floor((0..3).minOf { vertices[offset + it].u }.toDouble() * 4)
                for (i in 0..3) {
                    val v = vertices[offset + i]
                    val p = points[i]
                    val alpha = coverage * (.2f + v.rubber * .8f)
                    buffer.vertex(pose.pose(), (p.x + normal.x * .002).toFloat(),
                        (p.y + normal.y * .002).toFloat(), (p.z + normal.z * .002).toFloat())
                        .color(v.shade * coating.color.x, v.shade * coating.color.y, v.shade * coating.color.z, alpha)
                        .uv(sprite.getU(((v.u * 4 - tile) * 16).coerceIn(0.0, 16.0)), sprite.getV(v.v.toDouble() * 16))
                        .overlayCoords(overlay).uv2(light)
                        .normal(pose.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
                }
            }
        }
    }
}
