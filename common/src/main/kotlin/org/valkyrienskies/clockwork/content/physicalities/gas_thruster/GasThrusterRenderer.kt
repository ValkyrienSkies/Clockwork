package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import kotlin.math.sin
import kotlin.math.sqrt
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.DirectionalBlock
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity.Companion.NOZZLE_PIVOT
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity.Companion.NOZZLE_LENGTH

class GasThrusterRenderer : SafeBlockEntityRenderer<GasThrusterBlockEntity>() {
    companion object {
        private const val OPAQUE_PLUME_ALPHA = 150f / 255f
        private val mouthX = floatArrayOf(-1f, 1f, 1f, -1f)
        private val mouthZ = floatArrayOf(-1f, -1f, 1f, 1f)
    }

    override fun renderSafe(be: GasThrusterBlockEntity, partialTicks: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val heat = be.plumeHeat
        val glow = be.previousLipGlow + (be.lipGlow - be.previousLipGlow) * partialTicks
        val spread = be.previousHeatSpread + (be.heatSpread - be.previousHeatSpread) * partialTicks
        val outward = be.blockState.getValue(DirectionalBlock.FACING).opposite
        val exhaust = be.exhaustDirection()
        pose.pushPose()
        pose.translate(0.5 + NOZZLE_PIVOT * outward.stepX, 0.5 + NOZZLE_PIVOT * outward.stepY, 0.5 + NOZZLE_PIVOT * outward.stepZ)
        if (glow >= 0.01f) {
            pose.pushPose()
            pose.mulPose(outward.rotation)
            pose.translate(0.0, NOZZLE_LENGTH, 0.0)
            renderHeat(pose, buffers.getBuffer(ClockworkRenderTypes.NOZZLE_HEAT), glow, spread, moving = false)
            pose.popPose()
        }
        val rotation = Quaternionf().rotationTo(
            outward.stepX.toFloat(), outward.stepY.toFloat(), outward.stepZ.toFloat(),
            exhaust.x.toFloat(), exhaust.y.toFloat(), exhaust.z.toFloat()
        ).mul(outward.rotation)
        pose.mulPose(rotation)
        val impact = be.plumeImpact
        val normal = impact?.normal?.let { rotation.transformInverse(Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())) }
        val contact = (impact?.distance?.toFloat() ?: 0f) + 0.16f
        pose.pushPose()
        pose.mulPose(Axis.XP.rotationDegrees(-90f))
        pose.translate(-0.5, -0.5, -0.5 - NOZZLE_PIVOT)
        CachedBuffers.partial(ClockworkPartials.GAS_THRUSTER_NOZZLE, be.blockState)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .renderInto(pose, buffers.getBuffer(RenderType.cutout()))
        pose.popPose()
        pose.translate(0.0, NOZZLE_LENGTH, 0.0)
        if (glow >= 0.01f) {
            renderHeat(pose, buffers.getBuffer(ClockworkRenderTypes.NOZZLE_HEAT), glow, spread, moving = true)
        }
        pose.translate(0.0, -0.16, 0.0)
        val matrix = pose.last().pose()
        val stellar = be.stellarSteam
        val time = (((be.level?.gameTime ?: 0L) % 4096) / 2 * 2 + be.blockPos.asLong() % 197) * 0.7f
        if (be.vaporStrength > 0.01f) {
            val vapor = buffers.getBuffer(ClockworkRenderTypes.THRUSTER_VAPOR)
            val strength = be.vaporStrength
            val length = be.plumeLength
            drawSleeve(vapor, matrix, normal, contact, 0.10f, 0.16f + 0.48f * length, 0.27f,
                0.56f * strength, 0f, 0.78f + 0.22f * stellar, 0.85f, 0.89f - 0.45f * stellar)
            val tick = (be.level?.gameTime ?: 0L) + be.blockPos.asLong() % 32
            for (i in 0 until 4) {
                val age = (Math.floorMod(tick + i * 8, 32L) + partialTicks) / 32f
                val center = -0.06f + (0.22f + length) * age
                val width = 0.29f * (1f - 0.78f * age)
                val depth = width * 0.65f
                val opacity = strength * (1f - age)
                drawSleeve(vapor, matrix, normal, contact, center - depth, center + depth, width,
                    opacity * 0.40f, opacity * 0.24f, 0.77f + 0.20f * stellar,
                    0.84f - 0.45f * stellar, 0.89f + 0.11f * stellar)
            }
        }
        if (heat >= 0.01f || stellar >= 0.01f) {

            val colorHeat = heat
            val blueShift = ((colorHeat - 0.90f) / 0.10f).coerceIn(0f, 1f)
            val red = 1f - 0.78f * blueShift
            val green = (0.32f + 0.65f * colorHeat) * (1f - 0.28f * blueShift)
            val amberBlue = 0.06f + 0.79f * colorHeat * colorHeat
            val blue = amberBlue + (1f - amberBlue) * blueShift
            val intensity = minOf(1f, heat * 2f) * sqrt(be.exhaustFraction)
            val dryExhaust = (be.exhaustFraction - 8f * be.steamFraction).coerceAtLeast(0f)
            val pulse = sin(time * 0.91f + 0.85f * sin(time * 0.37f)) * 0.65f + sin(time * 1.73f) * 0.35f
            val swing = 0.30f * pulse * heat * heat * dryExhaust
            val length = 0.16f + be.plumeLength * (1f + 0.16f * swing)
            val base = (4.5f + 0.48f * swing) / 16f
            val violet = be.violetExhaust
            fun sleeves(type: RenderType, minAlpha: Float = 0.01f) {
                val vertices = buffers.getBuffer(type)
                val seed = (be.blockPos.asLong() and 15L).toInt()
                if (stellar >= minAlpha) {
                    drawSleeve(vertices, matrix, normal, contact, 0.10f, 0.16f + 0.72f * be.plumeLength,
                        0.14f, stellar, stellar, 1f, 0.72f, 0.22f, 2, seed)
                    drawSleeve(vertices, matrix, normal, contact, 0.10f, 0.16f + 0.52f * be.plumeLength,
                        0.10f, stellar, stellar, 1f, 0.95f, 0.80f, 4, seed)
                }
                if (heat < 0.01f || intensity < minAlpha) return
                for (layer in 0 until 5) {
                    val radius = base * (1f - 0.208f * layer)
                    val stop = 0.10f + (length - 0.10f) * (0.24f + 0.19f * layer)
                    drawSleeve(vertices, matrix, normal, contact, 0.10f, stop, radius, intensity, intensity,
                        red + (0.64f - red) * violet, green + (0.21f - green) * violet, blue, layer, seed)
                }
                if (violet >= 0.01f && intensity * violet >= minAlpha) {
                    for (corner in mouthX.indices) {
                        drawSleeve(vertices, matrix, normal, contact, 0.10f, 0.10f + 0.42f * (length - 0.10f),
                            0.065f, intensity * violet, intensity * violet, 0.77f, 0.20f, 1f, 1, seed + corner,
                            mouthX[corner] * base * 1.45f, mouthZ[corner] * base * 1.45f)
                    }
                }
            }
            sleeves(ClockworkRenderTypes.THRUSTER_PLUME)
            sleeves(ClockworkRenderTypes.THRUSTER_PLUME_GLOW)
            if (maxOf(stellar, intensity) >= OPAQUE_PLUME_ALPHA) {
                sleeves(ClockworkRenderTypes.THRUSTER_PLUME_DEPTH, OPAQUE_PLUME_ALPHA)
            }
        }
        pose.popPose()
    }

    private fun renderHeat(pose: PoseStack, vertices: VertexConsumer, heat: Float, spread: Float, moving: Boolean) {
        val front = if (moving) 0f else -0.5f
        val reach = 0.12f + spread * 1.05f
        if (reach <= -front) return
        val back = if (moving) -0.8125f else -1f
        val step = if (moving) 0f else 1 / 16f
        pose.pushPose()
        repeat(4) { side ->
            val matrix = pose.last().pose()
            fun plate(width: Float, inner: Float, outer: Float) {
                fun point(x: Float, y: Float, z: Float, brightness: Float) {
                    vertices.vertex(matrix, x, y, z).color(heat, if (moving) 1f else 0f, spread, brightness)
                        .uv(x * 16f + side, -y * 16f).endVertex()
                }
                val rim = front + 0.001f
                point(-width, rim, inner, 0.96f)
                point(width, rim, inner, 0.96f)
                point(width, rim, outer, 0.78f)
                point(-width, rim, outer, 0.78f)
                fun surface(z: Float, brightness: Float) {
                    point(-width, front, z, brightness)
                    point(width, front, z, brightness)
                    point(width, back, z, brightness)
                    point(-width, back, z, brightness)
                }
                surface(outer + 0.001f, 0.78f)
                surface(inner - 0.001f, 0.90f)
            }
            plate((if (side % 2 == 0) 0.375f else 0.3125f) + step, 0.3125f + step, 0.375f + step)
            plate(0.25f, 0.375f + step, 0.4375f + step)
            pose.mulPose(Axis.YP.rotationDegrees(90f))
        }
        pose.popPose()
    }

    private fun drawSleeve(vertices: VertexConsumer, matrix: Matrix4f, normal: Vector3f?, contact: Float,
                          start: Float, end: Float, width: Float, alpha: Float, endAlpha: Float,
                          red: Float, green: Float, blue: Float, layer: Int = -1, seed: Int = 0,
                          tipX: Float = 0f, tipZ: Float = 0f) {
        for (i in mouthX.indices) {
            val next = (i + 1) % mouthX.size
            val shade = when (i % 4) {
                0 -> 0.78f
                2 -> 1f
                else -> 0.90f
            }
            fun ring(side: Int, t: Float) {
                val radius = width
                val distance = start + (end - start) * t
                val opacity = alpha + (endAlpha - alpha) * t
                var x = mouthX[side] * radius + tipX * t
                var y = distance
                var z = mouthZ[side] * radius + tipZ * t
                if (normal != null) {
                    val depth = 0.018f - (normal.x * x + normal.y * (y - contact) + normal.z * z)
                    if (depth > 0f) {
                        val radial = normal.x * mouthX[side] + normal.z * mouthZ[side]
                        val spread = depth * 0.65f
                        x += depth * normal.x + spread * (mouthX[side] - radial * normal.x)
                        y += depth * normal.y - spread * radial * normal.y
                        z += depth * normal.z + spread * (mouthZ[side] - radial * normal.z)
                    }
                }
                val vertex = vertices.vertex(matrix, x, y, z).color(red, green, blue, if (layer >= 0) opacity else opacity * shade)
                if (layer >= 0) {
                    val face = (seed * 5 + layer) * 4 + i
                    vertex.uv(2f * face + if (side == i) 0f else 1f, t)
                        .uv2((width * 512f).toInt(), ((end - start) * 256f).toInt())

                }
                vertex.endVertex()
            }
            fun crossing(side: Int): Float {
                if (normal == null) return 1f
                val radial = normal.x * mouthX[side] + normal.z * mouthZ[side]
                val from = radial * width + normal.y * (start - contact) - 0.018f
                val to = radial * width + normal.y * (end - contact) + normal.x * tipX + normal.z * tipZ - 0.018f
                return if (from * to < 0f) from / (from - to) else 1f
            }
            fun strip(from: Float, to: Float) {
                if (from >= to) return
                ring(i, from)
                ring(i, to)
                ring(next, to)
                ring(next, from)
            }
            val left = crossing(i)
            val right = crossing(next)
            val first = minOf(left, right)
            val last = maxOf(left, right)
            strip(0f, first)
            strip(first, last)
            strip(last, 1f)
        }
    }
}
