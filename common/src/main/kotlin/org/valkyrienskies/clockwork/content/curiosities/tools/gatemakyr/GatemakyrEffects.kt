package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11
import org.valkyrienskies.clockwork.content.schematics.VdexPreviewRenderState
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.LightTexture
import com.mojang.blaze3d.vertex.BufferBuilder
import net.minecraft.world.level.block.Blocks
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.render.RenderUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.InfuserRibbon
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronEffects
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.mixin.content.gravitron.GameRendererAccessor
import org.valkyrienskies.clockwork.util.render.EmissiveEnergyVertexConsumer
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import kotlin.math.*

object GatemakyrEffects {
    private val fillType = RenderType.entityCutoutNoCull(ResourceLocation("minecraft", "textures/misc/white.png"))
    private val faces = arrayOf(intArrayOf(0, 1, 3, 2), intArrayOf(4, 6, 7, 5), intArrayOf(0, 4, 5, 1),
        intArrayOf(2, 3, 7, 6), intArrayOf(0, 2, 6, 4), intArrayOf(1, 5, 7, 3))

    private class AdditiveConsumer(private val delegate: VertexConsumer) : VertexConsumer by delegate {
        override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
            delegate.vertex(x, y, z)
            return this
        }
        override fun vertex(matrix: Matrix4f, x: Float, y: Float, z: Float): VertexConsumer {
            delegate.vertex(matrix, x, y, z)
            return this
        }
        override fun color(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
            delegate.color(r * a / 255, g * a / 255, b * a / 255, 255)
            return this
        }
        override fun color(r: Float, g: Float, b: Float, a: Float): VertexConsumer =
            color((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), (a.coerceIn(0f, 1f) * 255).toInt())
        override fun uv(u: Float, v: Float): VertexConsumer {
            delegate.uv(u, v)
            return this
        }
    }

    private fun energyBuffer(buffers: MultiBufferSource): VertexConsumer {
        if (buffers === gateBuffers) return AdditiveConsumer(buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY))
        val consumer = ClockworkRenderTypes.energyBuffer(buffers)
        return if (ShaderPackCompat.enabled()) consumer else AdditiveConsumer(consumer)
    }

    fun color(side: Int, bright: Boolean = false): Int = if (bright) 0xffd9f7 else if (side == 0) 0xa66bff else 0xf06dce

    private fun noise(seed: Int): Double = ((sin(seed * 127.1 + 311.7) * 43758.5453).let { it - floor(it) })

    fun cube(ms: PoseStack, vc: VertexConsumer, center: Vec3, size: Double, rgb: Int, alpha: Float,
             rotation: Quaternionf? = null) {
        val points = (0..7).map { index ->
            val p = Vector3f(if (index and 4 == 0) -1f else 1f, if (index and 2 == 0) -1f else 1f, if (index and 1 == 0) -1f else 1f)
            rotation?.transform(p)
            center.add(p.x * size / 2, p.y * size / 2, p.z * size / 2)
        }
        for ((index, face) in faces.withIndex()) quad(ms, vc, face.map { points[it] }, shade(rgb, 0.65 + index * 0.07), alpha)
    }

    private fun glowCube(ms: PoseStack, vc: VertexConsumer, center: Vec3, size: Double, rgb: Int, alpha: Float,
                         rotation: Quaternionf? = null) {
        cube(ms, vc, center, size * 2.0, rgb, alpha * 0.07f, rotation)
        cube(ms, vc, center, size * 1.4, rgb, alpha * 0.2f, rotation)
        cube(ms, vc, center, size, rgb, alpha, rotation)
    }

    private fun shade(rgb: Int, amount: Double): Int =
        (((rgb shr 16 and 255) * amount).toInt().coerceIn(0, 255) shl 16) or
            (((rgb shr 8 and 255) * amount).toInt().coerceIn(0, 255) shl 8) or ((rgb and 255) * amount).toInt().coerceIn(0, 255)

    private fun quad(ms: PoseStack, vc: VertexConsumer, points: List<Vec3>, rgb: Int, alpha: Float = 1f) {
        for (p in points) vc.vertex(ms.last().pose(), p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
            .color(rgb shr 16 and 255, rgb shr 8 and 255, rgb and 255, (alpha.coerceIn(0f, 1f) * 255).toInt())
            .uv(0.5f, 0.5f).endVertex()
    }

    fun matrix(ms: PoseStack, buffers: MultiBufferSource, center: Vec3, time: Float, charge: Float, side: Int) {
        if (charge <= 0.001f) return
        val vc = energyBuffer(buffers)
        glowCube(ms, vc, center, 0.075 * charge, color(side, true), charge * 0.4f,
            Quaternionf().rotationXYZ(time * 0.17f, -time * 0.23f, time * 0.11f))
        for (i in 0 until 35) {
            val x = (i % 5 - 2) * 0.055
            val y = (i / 5 % 3 - 1) * 0.055
            val z = (i / 15 - 1) * 0.055
            val phase = time * (0.09 + noise(i) * 0.06) + i * 2.4
            val spread = (1 - charge) * 0.22 + 0.012
            val p = center.add(x * charge + cos(phase) * spread, y * charge + sin(phase * 1.3) * spread,
                z * charge + sin(phase * 0.7) * spread)
            val rotation = Quaternionf().rotationXYZ((phase * 0.8).toFloat(), phase.toFloat(), (phase * 0.6).toFloat())
            glowCube(ms, vc, p, (0.035 + 0.04 * noise(i + 50)) * charge, color(side, i % 6 == 0), 0.2f + charge * 0.12f, rotation)
        }
        for (arm in 0..3) for (j in 0..7) {
            val angle = arm * PI / 2
            val t = j / 8.0
            val pulse = ((time * 0.08 + arm * 0.23) % 1.0)
            val r = (1 - t) * (0.23 + charge * 0.1)
            val p = center.add(round(cos(angle) * r * 64) / 64, round(sin(angle) * r * 64) / 64,
                round(sin(t * PI * 3 + time * 0.3 + arm) * 2) / 64)
            glowCube(ms, vc, p, 0.018 + 0.012 * charge, color(side, abs(t - pulse) < 0.28), charge * 0.5f)
        }
    }

    private fun projectileRotation(time: Float, layer: Int) = Quaternionf().rotationXYZ(
        time * (0.11f - layer * 0.035f), time * (0.08f + layer * 0.047f), -time * (0.13f - layer * 0.025f))

    private fun projectileCore(ms: PoseStack, buffers: MultiBufferSource, center: Vec3, time: Float, side: Int) {
        val models = listOf(ClockworkPartials.CRYSTAL_INNER, ClockworkPartials.CRYSTAL, ClockworkPartials.CRYSTAL_OUTER)
        val types = listOf(RenderType.endPortal(), ClockworkRenderTypes.CRYSTAL.apply(RenderUtil.CRYSTAL_MATRIX),
            RenderType.entityTranslucent(RenderUtil.PURPLE_HUE))
        for (i in models.indices) {
            ms.pushPose()
            ms.translate(center.x, center.y, center.z)
            ms.mulPose(projectileRotation(time, i))
            ms.scale(3.4f, 3.4f, 3.4f)
            ms.translate(-0.5, -12.5 / 16, -0.5)
            CachedBuffers.partial(models[i], Blocks.AIR.defaultBlockState()).light<SuperByteBuffer>(LightTexture.FULL_BRIGHT)
                .disableDiffuse<SuperByteBuffer>().renderInto(ms, buffers.getBuffer(types[i]))
            (buffers as? MultiBufferSource.BufferSource)?.endBatch(types[i])
            ms.popPose()
        }
        val vc = energyBuffer(buffers)
        val sizes = doubleArrayOf(1.7, 3.2, 3.5)
        val strengths = floatArrayOf(0.9f, 0.55f, 0.28f)
        for (i in sizes.indices) {
            ms.pushPose()
            ms.translate(center.x, center.y, center.z)
            ms.mulPose(projectileRotation(time, i).rotateY(-PI.toFloat() / 4))
            if (i == 0) ms.scale(1f, 1.68f / 1.7f, 1f)
            glowCube(ms, vc, Vec3.ZERO, sizes[i] / 16 * 3.4 * 1.01, color(side, i < 2), strengths[i])
            ms.popPose()
        }
    }

    fun firingPulse(ms: PoseStack, buffers: MultiBufferSource, center: Vec3, age: Float, side: Int) {
        if (age !in 0f..10f) return
        val vc = energyBuffer(buffers)
        for (ring in 0..1) {
            val t = ((age - ring * 2) / 8).coerceIn(0f, 1f)
            if (age < ring * 2 || t >= 1) continue
            val radius = 0.08 + t * 0.55
            val width = 0.025 * (1 - t)
            val z = center.z - t * 0.16
            for (i in 0 until 32) {
                val a = i * PI / 16; val b = (i + 1) * PI / 16
                fun point(angle: Double, r: Double) = Vec3(center.x + cos(angle) * r, center.y + sin(angle) * r, z)
                for (layer in 0..1) {
                    val thickness = width * if (layer == 0) 3.5 else 1.0
                    quad(ms, vc, listOf(point(a, radius - thickness), point(a, radius + thickness),
                        point(b, radius + thickness), point(b, radius - thickness)), color(side, layer == 1),
                        (1 - t) * if (layer == 0) 0.15f else 0.7f)
                }
            }
        }
    }

    private val gateBuffers = MultiBufferSource.immediate(BufferBuilder(65536))
    private val worldProjection = Matrix4f()

    fun prepareShaderComposite(projection: Matrix4f) {
        worldProjection.set(projection)
    }

    @JvmStatic fun composite(ms: PoseStack, pt: Float) {
        if (GatemakyrClient.rifts.isEmpty() && GatemakyrClient.closing.isEmpty() && GatemakyrClient.flights.isEmpty() && GatemakyrClient.preview == null) return
        if (!ShaderPackCompat.enabled() || ShaderPackCompat.shadowPass()) return
        val mc = Minecraft.getInstance()
        val projection = Matrix4f(RenderSystem.getProjectionMatrix())
        val sorting = RenderSystem.getVertexSorting()
        VdexPreviewRenderState().use {
            try {
                mc.mainRenderTarget.bindWrite(true)
                RenderSystem.setProjectionMatrix(worldProjection, sorting)
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11.GL_LEQUAL)
                render(ms, gateBuffers, mc.gameRenderer.mainCamera.position, pt)
                gateBuffers.endBatch()
                GravitronEffects.render(ms, gateBuffers, pt, afterGates = true)
            } finally {
                RenderSystem.setProjectionMatrix(projection, sorting)
            }
        }
    }

    internal fun renderProjectedBeams(ms: PoseStack, camera: Vec3, pt: Float) {
        renderFlights(ms, gateBuffers, camera, pt, true)
        gateBuffers.endBatch()
        GravitronEffects.render(ms, gateBuffers, pt, camera, afterGates = true)
    }

    private fun renderRifts(ms: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, pt: Float) {
        val level = Minecraft.getInstance().level ?: return
        val now = level.gameTime
        val time = (now % 24000).toFloat() + pt
        val nearby = GatemakyrClient.rifts.values.mapNotNull { rift -> rift.packet.target?.frame(level, true)?.let { rift to it } }
            .filter { it.second.position.distanceToSqr(camera) <= GatemakyrTarget.SURFACE_RANGE * GatemakyrTarget.SURFACE_RANGE }
            .sortedBy { it.second.position.distanceToSqr(camera) }.take(16)
        for (outline in listOf(false, true)) {
            val riftBuffer = if (outline) AdditiveConsumer(buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY))
                else EmissiveEnergyVertexConsumer(buffers.getBuffer(fillType))
            for ((rift, frame) in nearby) {
                val age = (now - rift.packet.startedAt + pt).coerceAtLeast(0f)
                if (outline || !GatemakyrView.available(rift.packet.owner, rift.packet.side))
                    rift(ms, riftBuffer, frame, camera, age, time, rift.packet.side, outline, rift.packet.target!!)
            }
            for (closing in GatemakyrClient.closing) {
                val frame = closing.target.frame(level, true) ?: continue
                if (frame.position.distanceToSqr(camera) > GatemakyrTarget.SURFACE_RANGE * GatemakyrTarget.SURFACE_RANGE) continue
                rift(ms, riftBuffer, frame, camera, (16f - (now - closing.startedAt + pt)).coerceAtLeast(0f), time, closing.side, outline, closing.target, true)
            }
            if (!outline) {
                buffers.endBatch(fillType)
                for ((rift, frame) in nearby) if (GatemakyrView.available(rift.packet.owner, rift.packet.side)) {
                    GatemakyrView.draw(ms, frame, camera, rift, (now - rift.packet.startedAt + pt).coerceAtLeast(0f), time)
                }
            }
        }
    }

    fun render(ms: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, pt: Float) {
        if (ShaderPackCompat.shadowPass() || (buffers !== gateBuffers && ShaderPackCompat.enabled())) return
        renderRifts(ms, buffers, camera, pt)
        renderFlights(ms, buffers, camera, pt, false)
        renderPreview(ms, buffers, camera, pt)
    }

    private fun renderFlights(ms: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, pt: Float, projected: Boolean) {
        if (GatemakyrClient.flights.isEmpty()) return
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val now = level.gameTime
        val time = (now % 24000).toFloat() + pt
        val inverseView = Matrix4f(ms.last().pose()).invert()
        var vc = energyBuffer(buffers)
        for (flight in GatemakyrClient.flights) {
            val packet = flight.packet
            val end = packet.target?.frame(level, true)?.impact ?: continue
            if (end.distanceToSqr(camera) > 128.0 * 128.0 && packet.origin.distanceToSqr(camera) > 128.0 * 128.0 &&
                packet.crossings.none { (a, b) -> minOf(a.distanceToSqr(camera), b.distanceToSqr(camera)) < 128.0 * 128.0 }) continue
            if (!projected && flight.origin == null) {
                val state = GatemakyrClient.state(level.getPlayerByUUID(packet.owner))
                val captured = state?.muzzle
                if (captured != null && now - state.muzzleTime <= 2) {
                    val p = Vector3f(captured.x.toFloat(), captured.y.toFloat(), captured.z.toFloat())
                    if (state.muzzleFirstPerson) {
                        val renderer = mc.gameRenderer as GameRendererAccessor
                        val view = mc.gameRenderer.mainCamera
                        val ratio = tan(renderer.`clockwork$getFov`(view, pt, true) * PI / 360) /
                            tan(renderer.`clockwork$getFov`(view, pt, false) * PI / 360)
                        p.x *= ratio.toFloat(); p.y *= ratio.toFloat()
                    }
                    inverseView.transformPosition(p)
                    flight.origin = camera.add(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
                } else flight.origin = packet.origin
            }
            val start = flight.origin ?: packet.origin
            val age = (now - packet.startedAt + pt).coerceAtLeast(0f)
            val progress = (age / packet.duration).coerceIn(0f, 1f)
            val center = packet.flightPoint(start, end, progress.toDouble()).subtract(camera)
            if (age <= packet.duration) {
                projectileCore(ms, buffers, center, time, packet.side)
                vc = energyBuffer(buffers)
                for (i in 0..5) {
                    val phase = time * (0.18 + noise(i) * 0.15) + i
                    val p = center.add(cos(phase) * 0.42, sin(phase * 1.4) * 0.36, cos(phase * 0.8) * 0.4)
                    glowCube(ms, vc, p, 0.06 + noise(i + 7) * 0.04, color(packet.side), 0.3f,
                        Quaternionf().rotationXYZ(phase.toFloat(), (phase * 1.7).toFloat(), (phase * 0.6).toFloat()))
                }
                for (i in 1..24) {
                    val t = ((age - i * 0.22) / packet.duration).coerceIn(0.0, 1.0)
                    if (t >= progress) continue
                    val fade = 1 - i / 25f
                    val p = packet.flightPoint(start, end, t).subtract(camera).add(sin(i * 5.0 + time) * 0.06,
                        cos(i * 3.0 + time) * 0.06, sin(i + time * 0.7) * 0.06)
                    glowCube(ms, vc, p, 0.045 + fade * 0.17, color(packet.side, i < 6), fade * 0.55f,
                        Quaternionf().rotationXYZ(i + time, i - time, time * 0.8f))
                }
            } else {
                val burst = (age - packet.duration) / 18f
                if (burst > 1) continue
                for (i in 0..35) {
                    val direction = Vec3(noise(i * 3) - 0.5, noise(i * 3 + 1) - 0.5, noise(i * 3 + 2) - 0.5).normalize()
                    glowCube(ms, vc, end.subtract(camera).add(direction.scale(sqrt(burst.toDouble()) * (0.7 + noise(i) * 1.6))),
                        (1 - burst) * 0.17, color(packet.side, i % 5 == 0), 1 - burst,
                        Quaternionf().rotationXYZ(i + time, time * 0.6f, i - time))
                }
            }
        }
    }

    fun renderPreview(ms: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, pt: Float) {
        val target = GatemakyrClient.preview ?: return
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val frame = target.frame(level, true) ?: return
        val state = GatemakyrClient.state(mc.player) ?: return
        val charge = state.animation.sample(level.gameTime, pt).charge
        val vc = energyBuffer(buffers)
        val origin = frame.position.subtract(camera).add(frame.normal.scale(0.012))
        val width = target.width.toDouble()
        val height = target.height.toDouble()
        val stroke = (origin.length() * 0.0025).coerceIn(1.0 / 16.0, 0.125)
        val tint = color(state.animation.side, false)
        val opacity = 0.78f + 0.22f * charge
        for (sign in listOf(-1, 1)) {
            val horizontal = origin.add(frame.v.scale(sign * (height - stroke) * 0.5))
            val vertical = origin.add(frame.u.scale(sign * (width - stroke) * 0.5))
            tile(ms, vc, horizontal, frame.u, frame.v, width, stroke, tint, opacity)
            tile(ms, vc, vertical, frame.u, frame.v, stroke, height - 2 * stroke, tint, opacity)
        }
        for (i in 0..3) {
            val angle = i * PI / 2
            val radius = 0.12 + (1 - charge) * 0.05
            val center = frame.impact.subtract(camera).add(frame.normal.scale(0.02))
                .add(frame.u.scale(cos(angle) * radius)).add(frame.v.scale(sin(angle) * radius))
            tile(ms, vc, center, frame.u, frame.v, 0.045, 0.045, color(state.animation.side, charge >= 1f))
        }
    }

    private fun tile(ms: PoseStack, vc: VertexConsumer, center: Vec3, u: Vec3, v: Vec3, width: Double, height: Double,
                     rgb: Int, alpha: Float = 1f) {
        val a = u.scale(width / 2); val b = v.scale(height / 2)
        quad(ms, vc, listOf(center.subtract(a).subtract(b), center.add(a).subtract(b), center.add(a).add(b), center.subtract(a).add(b)), rgb, alpha)
    }

    private fun energyStroke(ms: PoseStack, vc: VertexConsumer, points: List<Vec3>, widths: List<Float>,
                             alphas: List<Float>, side: Int) {
        val matrix = ms.last().pose()
        InfuserRibbon.draw(vc, matrix, points, widths.map { it * 3.2f }, alphas.map { it * 0.19f },
            if (side == 0) 0.60f else 0.78f, 0.34f, if (side == 0) 0.86f else 0.72f)
        InfuserRibbon.draw(vc, matrix, points, widths, alphas.map { it * 0.8f },
            if (side == 0) 0.765f else 0.94f, if (side == 0) 0.627f else 0.55f, 0.89f)
        InfuserRibbon.draw(vc, matrix, points, widths.map { it * 0.3f }, alphas, 0.98f, 0.88f, 1f)
    }

    private fun spark(ms: PoseStack, vc: VertexConsumer, center: Vec3, size: Double, alpha: Float) {
        val view = center.normalize()
        var right = view.cross(Vec3(0.0, 1.0, 0.0)).normalize()
        if (right.lengthSqr() < 1e-8) right = Vec3(1.0, 0.0, 0.0)
        val up = right.cross(view).normalize()
        val radii = doubleArrayOf(0.0, 0.28, 1.0)
        val opacity = floatArrayOf(1f, 0.38f, 0f)
        fun vertex(radius: Double, angle: Double, strength: Float) {
            val p = center.add(right.scale(cos(angle) * radius * size)).add(up.scale(sin(angle) * radius * size))
            vc.vertex(ms.last().pose(), p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                .color(0.98f, 0.88f, 1f, (alpha * strength).coerceIn(0f, 1f)).uv(0.5f, 0.5f).endVertex()
        }
        for (ring in 0..1) for (i in 0..7) {
            val a = i * PI / 4; val b = (i + 1) * PI / 4
            vertex(radii[ring], a, opacity[ring]); vertex(radii[ring + 1], a, opacity[ring + 1])
            vertex(radii[ring + 1], b, opacity[ring + 1]); vertex(radii[ring], b, opacity[ring])
        }
    }

    private fun rift(ms: PoseStack, vc: VertexConsumer, frame: GatemakyrTarget.Frame, camera: Vec3,
                     age: Float, time: Float, side: Int, outline: Boolean, target: GatemakyrTarget, closing: Boolean = false) {
        val reveal = GatemakyrAnimation.smooth(age / 16)
        val bounds = target.opening(frame, age, closing)
        val columns = ceil((bounds.maxX - bounds.minX) * 16).toInt().coerceIn(1, 64)
        val rows = ceil((bounds.maxY - bounds.minY) * 16).toInt().coerceIn(1, 64)
        val cellWidth = (bounds.maxX - bounds.minX) / columns
        val cellHeight = (bounds.maxY - bounds.minY) / rows
        val origin = frame.position.subtract(camera)
        val facing = if (frame.normal.dot(camera.subtract(frame.position)) >= 0) 1.0 else -1.0
        fun point(x: Double, y: Double, z: Double = 0.0) =
            origin.add(frame.u.scale(x)).add(frame.v.scale(y)).add(frame.normal.scale(z))
        if (!outline) {
            for (y in 0 until rows) for (x in 0 until columns) {
                if (age < 14 && noise(x * 31 + y * 17 + floor(age * 2).toInt()) > 0.45 + age / 25) continue
                val px = bounds.minX + (x + 0.5) * cellWidth
                val py = bounds.minY + (y + 0.5) * cellHeight
                val radius = hypot(px / (target.width / 2.0), py / (target.height / 2.0))
                val swirl = (sin(atan2(py, px) * 3 + radius * 8 - time * 0.08) + 1) / 2
                val interference = sin(py * 23 + time * 0.21) * sin(px * 17 - time * 0.14) * 0.06
                val brightness = (0.1 + swirl * 0.4 + interference).coerceAtLeast(0.0)
                val rgb = ((10 + brightness * 45).toInt() shl 16) or ((3 + brightness * 13).toInt() shl 8) or (18 + brightness * 78).toInt()
                tile(ms, vc, point(px, py), frame.u, frame.v, cellWidth, cellHeight, rgb)
            }
            return
        }
        val w = (bounds.maxX - bounds.minX) / 2
        val h = (bounds.maxY - bounds.minY) / 2
        val centerX = (bounds.maxX + bounds.minX) / 2
        val centerY = (bounds.maxY + bounds.minY) / 2
        val perimeter = 4 * (w + h)
        fun edge(t: Double): Vec3 {
            val distance = ((t % 1 + 1) % 1) * perimeter
            return when {
                distance < 2 * w -> Vec3(-w + distance, -h, 0.0)
                distance < 2 * w + 2 * h -> Vec3(w, -h + distance - 2 * w, 0.0)
                distance < 4 * w + 2 * h -> Vec3(w - (distance - 2 * w - 2 * h), h, 0.0)
                else -> Vec3(-w, h - (distance - 4 * w - 2 * h), 0.0)
            }
        }
        for (strand in 0..2) {
            val widths = ArrayList<Float>()
            val alphas = ArrayList<Float>()
            val points = (0..128).map { i ->
                val t = i / 128.0
                val p = edge(t)
                val outward = Vec3(p.x / w, p.y / h, 0.0).normalize()
                val phase = t * PI * 14 - time * 0.22 + strand * PI * 2 / 3
                val amplitude = if (strand == 0) 0.006 else 0.025 + (1 - reveal) * 0.06
                val jitter = sin(t * PI * 46 + time * 0.5 + strand) * amplitude * 0.35
                val offset = sin(phase) * amplitude + jitter
                val pulse = (sin(t * PI * 6 - time * 0.17 + strand) * 0.5 + 0.5).toFloat().pow(6)
                widths.add((if (strand == 0) 0.018f else 0.008f) * (0.85f + pulse * 0.8f))
                alphas.add((if (strand == 0) 0.45f else 0.16f) + pulse * (if (strand == 0) 0.25f else 0.1f))
                point(centerX + p.x + outward.x * offset, centerY + p.y + outward.y * offset,
                    facing * (0.015 + if (strand == 0) 0.0 else (cos(phase) + 1) * amplitude))
            }
            energyStroke(ms, vc, points, widths, alphas, side)
        }
        for (i in 0 until 20) {
            val cycle = ((time * 0.035 + noise(i * 31) * 7) % 1.0)
            val p = edge(noise(i * 17) + cycle * 0.025)
            val outward = Vec3(p.x / w, p.y / h, 0.0).normalize()
            val drift = sin(cycle * PI) * (0.025 + noise(i + 6) * 0.13)
            val center = point(centerX + p.x + outward.x * drift, centerY + p.y + outward.y * drift,
                facing * (0.025 + cycle * 0.08))
            spark(ms, vc, center, 0.025 + noise(i + 3) * 0.04, (sin(cycle * PI) * 0.75).toFloat())
        }
        if (age < 18) {
            val flash = (1 - age / 18).coerceIn(0f, 1f)
            val spread = GatemakyrAnimation.smooth(age / 18) * 0.32
            val points = (0..128).map { i ->
                val p = edge(i / 128.0)
                point(centerX + p.x * (1 + spread), centerY + p.y * (1 + spread), facing * 0.02)
            }
            energyStroke(ms, vc, points, List(129) { 0.012f + flash * 0.03f }, List(129) { flash * 0.7f }, side)
        }
    }
}
