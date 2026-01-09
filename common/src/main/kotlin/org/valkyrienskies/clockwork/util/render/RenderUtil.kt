package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.arc.ArcBias
import org.valkyrienskies.clockwork.util.arc.LightningBolt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object RenderUtil {

    val CRYSTAL_MATRIX = ClockworkMod.asResource("textures/block/empty.png");
    val PURPLE_HUE = ClockworkMod.asResource("textures/block/purple_hue.png")

    /**
     * Renders three cubes making up the Core with the PartialItemModelRenderer. Adds offset to make up for natural model transformation pivot
     * @param innerData Data for inner cube offset and rotation
     * @param data Data for middle and outer cube's offset and rotation
     */
    fun renderCubeMatrix(matrices: PoseStack, renderer: PartialItemModelRenderer, innerData: TransformData, data: TransformData, scale: Float, light: Int) {
        var modelOffset = org.joml.Vector3f(0f, -4.5f / 16.0f, 0f)

        renderAndTransform(matrices, ClockworkPartials.CRYSTAL_INNER, RenderType.endPortal(), renderer, modelOffset, innerData.offset, innerData.rotation, scale, light)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL, ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX), renderer, modelOffset, data.offset, data.rotation, scale, light)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL_OUTER, RenderType.entityTranslucent(PURPLE_HUE), renderer, modelOffset, data.offset, data.rotation, scale, light)
    }

    fun renderCube(matrices: PoseStack, renderer: PartialItemModelRenderer, data: TransformData, scale: Float, light: Int) {
        var modelOffset = org.joml.Vector3f(0f, -4.5f / 16.0f, 0f)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL, ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX), renderer, modelOffset, data.offset, data.rotation, scale, light)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL_OUTER, RenderType.entityTranslucent(PURPLE_HUE), renderer, modelOffset, data.offset, data.rotation, scale, light)
    }

    /**
     * Helper function for
     * @see RenderUtil.renderCubeMatrix
     * Transforms and renders a model
     */
    fun renderAndTransform(matrices: PoseStack, model: PartialModel, renderType: RenderType, renderer: PartialItemModelRenderer, modelCorrection: org.joml.Vector3f, offset: org.joml.Vector3f, rotationVec: org.joml.Vector3f, scale : Float, light: Int) {
        matrices.pushPose()
        matrices.translate(offset.x().toDouble(), offset.y().toDouble(), offset.z().toDouble())
        matrices.translate(0.25,0.25,0.25)
        matrices.pushPose()
        //Scale
        //val scale = 1.5f
        matrices.scale(scale, scale, scale)
        matrices.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))

        matrices.translate(-modelCorrection.x().toDouble(), -modelCorrection.y().toDouble(), -modelCorrection.z().toDouble())
        Quaternionf(AxisAngle4f(AngleHelper.rad(rotationVec.y().toDouble()), 0f, 1f, 0f))
        Quaternionf(AxisAngle4f(AngleHelper.rad(rotationVec.x().toDouble()), 1f, 0f, 0f))
        Quaternionf(AxisAngle4f(AngleHelper.rad(rotationVec.z().toDouble()), 0f, 0f, 1f))
        matrices.translate(modelCorrection.x().toDouble(), modelCorrection.y().toDouble(), modelCorrection.z().toDouble())
        renderer.render(model.get(), renderType, light)


        matrices.popPose()
        matrices.popPose()
    }

    /**
     * Renders three cubes making up the Core with the CachedBuffers using a BlockState.
     * @param innerData Data for inner cube offset and rotation
     * @param data Data for middle cube offset and rotation
     * @param outerData Data for outer cube offset and rotation
     */
    fun renderCubeMatrix(matrices: PoseStack, buffer: MultiBufferSource, blockState: BlockState, innerData: TransformData, data: TransformData, outerData: TransformData, scale: Float, light: Int, overlay: Int){
        val crystal_inner_buffer = buffer.getBuffer(RenderType.endPortal())
        val crystal_inner = CachedBuffers.partial(ClockworkPartials.CRYSTAL_INNER, blockState)
        renderAndTransform(crystal_inner, scale, innerData.offset, innerData.rotation).light<SuperByteBuffer>(light).color<SuperByteBuffer>(255,255,255, 255).overlay<SuperByteBuffer>(overlay).disableDiffuse<SuperByteBuffer>().renderInto(matrices, crystal_inner_buffer)

        val crystal_buffer = buffer.getBuffer(ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX))
        val crystal = CachedBuffers.partial(ClockworkPartials.CRYSTAL, blockState)
        renderAndTransform(crystal, scale, data.offset, data.rotation).light<SuperByteBuffer>(light).color<SuperByteBuffer>(255,255,255, 255).overlay<SuperByteBuffer>(overlay).disableDiffuse<SuperByteBuffer>().renderInto(matrices, crystal_buffer)

        val crystal_outer_buffer = buffer.getBuffer(RenderType.entityTranslucent(PURPLE_HUE))
        val crystal_outer = CachedBuffers.partial(ClockworkPartials.CRYSTAL_OUTER, blockState)
        renderAndTransform(crystal_outer, scale, outerData.offset, outerData.rotation).light<SuperByteBuffer>(light).color<SuperByteBuffer>(255,255,255, 255).overlay<SuperByteBuffer>(overlay).renderInto(matrices, crystal_outer_buffer)
    }

    /**
     * Helper function for
     * @see RenderUtil.renderCubeMatrix
     * Transforms and renders a model
     */
    private fun renderAndTransform(buffer: SuperByteBuffer, scale: Float, coreOffset: org.joml.Vector3f, coreRotation: org.joml.Vector3f): SuperByteBuffer {
        //Scale
        buffer.scale(scale)
        buffer.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))
        //Y
        buffer.translateY((coreOffset.y * 2)).rotateCentered((coreRotation.y / 180 * Math.PI).toFloat(), Direction.UP)
        //Z
        buffer.translateY((coreOffset.z * 2)).rotateCentered((coreRotation.z / 180 * Math.PI).toFloat(), Direction.NORTH)
        //X
        buffer.translateY((coreOffset.x * 2)).rotateCentered((coreRotation.x / 180 * Math.PI).toFloat(), Direction.EAST)

        buffer.translateY((-(4.5 / 16.0)).toFloat())
        return buffer
    }

    fun hash64(x: Long): Long {
        var z = x.toULong() + 0x9E3779B97F4A7C15uL
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        z = z xor (z shr 31)
        return z.toLong()
    }

    fun rand01(state: Long): Double {
        // 53-bit mantissa -> [0,1)
        val v = (hash64(state).toULong() shr 11).toLong()
        return v.toDouble() * (1.0 / (1L shl 53).toDouble())
    }

    private fun randSigned(state: Long): Double = rand01(state) * 2.0 - 1.0

    private fun subdivideOnce(pts: List<Vec3>, offsetMag: Double, seed: Long): List<Vec3> {
        if (pts.size < 2) return pts
        val out = ArrayList<Vec3>(pts.size * 2 - 1)

        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            out.add(a)

            val mid = a.add(b).scale(0.5)
            val dir = b.subtract(a)
            val len = dir.length()
            val dirN = if (len > 1e-6) dir.scale(1.0 / len) else Vec3(0.0, 1.0, 0.0)

            val ref = if (abs(dirN.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
            val u = dirN.cross(ref).normalize()
            val v = dirN.cross(u).normalize()

            val r0 = randSigned(seed + i * 1315423911L)
            val r1 = randSigned(seed + i * 2654435761L)
            val disp = u.scale(r0 * offsetMag).add(v.scale(r1 * offsetMag))

            out.add(mid.add(disp))
        }
        out.add(pts.last())
        return out
    }

    private fun animateWiggle(
        pts: List<Vec3>,
        start: Vec3,
        end: Vec3,
        seed: Long,
        time: Double,
        amount: Double
    ): List<Vec3> {
        if (pts.size <= 2) return pts

        val out = ArrayList<Vec3>(pts.size)
        out.add(start)

        val overallDir = end.subtract(start)
        val overallLen = overallDir.length().coerceAtLeast(1e-6)
        val overallN = overallDir.scale(1.0 / overallLen)

        val ref = if (abs(overallN.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val U = overallN.cross(ref).normalize()
        val V = overallN.cross(U).normalize()

        for (i in 1 until pts.size - 1) {
            val t = i.toDouble() / (pts.size - 1).toDouble()

            // smooth-ish oscillation per point
            //val f0 = 6.0 + 10.0 * rand01(seed + i * 11L)
            //val f1 = 6.0 + 10.0 * rand01(seed + i * 19L)
            val f0 = 14.0 + 22.0 * rand01(seed + i * 11L)
            val f1 = 14.0 + 22.0 * rand01(seed + i * 19L)

            val ph0 = rand01(seed + i * 23L) * Math.PI * 2.0
            val ph1 = rand01(seed + i * 29L) * Math.PI * 2.0

            val s0 = sin(time * f0 + ph0)
            val s1 = sin(time * f1 + ph1)

            // taper wiggle to zero at ends
            val taper = (t * (1.0 - t)) * 4.0

            val disp = U.scale(s0 * amount * taper).add(V.scale(s1 * amount * taper))
            out.add(pts[i].add(disp))
        }

        out.add(end)
        return out
    }

    private fun animateWiggleLocalTangent(
        pts: List<Vec3>,
        seed: Long,
        time: Double,
        amount: Double
    ): List<Vec3> {
        if (pts.size <= 2) return pts
        val out = ArrayList<Vec3>(pts.size)
        out.add(pts.first())

        for (i in 1 until pts.size - 1) {
            val prev = pts[i - 1]
            val cur = pts[i]
            val next = pts[i + 1]

            val tangent = next.subtract(prev)
            val len = tangent.length()
            val tN = if (len > 1e-6) tangent.scale(1.0 / len) else Vec3(0.0, 1.0, 0.0)

            val ref = if (kotlin.math.abs(tN.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
            val u = tN.cross(ref).normalize()
            val v = tN.cross(u).normalize()

            val f0 = 14.0 + 22.0 * rand01(seed + i * 11L)
            val f1 = 14.0 + 22.0 * rand01(seed + i * 19L)
            val ph0 = rand01(seed + i * 23L) * Math.PI * 2.0
            val ph1 = rand01(seed + i * 29L) * Math.PI * 2.0

            val s0 = kotlin.math.sin(time * f0 + ph0)
            val s1 = kotlin.math.sin(time * f1 + ph1)

            val tt = i.toDouble() / (pts.size - 1).toDouble()
            val taper = (tt * (1.0 - tt)) * 4.0

            val disp = u.scale(s0 * amount * taper).add(v.scale(s1 * amount * taper))
            out.add(cur.add(disp))
        }

        out.add(pts.last())
        return out
    }


    fun generatePolylinesFollow(
        inst: LightningBolt,
        nowGameTime: Long,
        partialTick: Float
    ): List<List<Vec3>> {
        val start = inst.startProvider()
        val end = inst.endProvider()

        // Backbone resolution: enough points so arc survives subdivision nicely
        val backboneSegments = 8

        // Base fractal from fixed seed (stable)
        var pts: List<Vec3> = buildBackbone(start, end, inst.arcBias, backboneSegments)
        var mag = inst.maxOffset
        repeat(inst.subdivisions) { s ->
            pts = subdivideOnce(pts, mag, inst.seed + s * 99991L)
            mag *= 0.55
        }

        // Add animated wiggle around the overall direction
        //val time = ((nowGameTime.toDouble() + partialTick) / 20.0) * 3.0
        val wiggleSpeed = 3.0
        val rawTime = ((nowGameTime.toDouble() + partialTick) / 20.0) * wiggleSpeed
        val time = kotlin.math.floor(rawTime * 12.0) / 12.0  // 12 steps per second


        pts = if (inst.arcBias == ArcBias.None) animateWiggle(
            pts = pts,
            start = start,
            end = end,
            seed = inst.seed xor 0xCAFEBABEL,
            time = time,
            amount = inst.maxOffset * 0.18 // small wiggle; keep the “shape”
        ) else {
            animateWiggleLocalTangent(
                pts = pts,
                seed = inst.seed xor 0xDEADBEEFL,
                time = time,
                amount = inst.maxOffset * 0.18
            )
        }

        val lines = ArrayList<List<Vec3>>()
        lines.add(pts)

        // branches, also animated a bit
        for (i in 1 until pts.size - 1) {
            if (rand01(inst.seed + i * 8191L) >= inst.branchChance) continue

            val p = pts[i]
            val trunk = pts[i + 1].subtract(pts[i - 1]).normalize()
            val ref = if (abs(trunk.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
            val u = trunk.cross(ref).normalize()
            val v = trunk.cross(u).normalize()

            val bdir = u.scale(randSigned(inst.seed + i * 17L)).add(v.scale(randSigned(inst.seed + i * 31L))).normalize()
            val trunkT = i.toDouble() / (pts.size - 1).toDouble()
            val branchTaper = if (inst.branchTaper) (1.0 - trunkT).coerceIn(0.0, 1.0) else 1.0
            val len = start.distanceTo(end) * inst.branchScale * (0.6 + 0.4 * rand01(inst.seed + i * 43L)) * branchTaper
            val bend = p.add(bdir.scale(len))

            var bPts: List<Vec3> = listOf(p, bend)
            var bMag = inst.maxOffset * inst.branchScale
            repeat((inst.subdivisions - 2).coerceAtLeast(2)) { s ->
                bPts = subdivideOnce(bPts, bMag, inst.seed + 0xABCDEF + i * 1009L + s * 131L)
                bMag *= 0.6
            }

            bPts = animateWiggle(
                pts = bPts,
                start = p,
                end = bend,
                seed = inst.seed xor (i.toLong() shl 32),
                time = time,
                amount = inst.maxOffset * 0.12 * inst.branchScale
            )

            lines.add(bPts)
        }

        return lines
    }

    fun addRibbonSegment(
        vc: VertexConsumer,
        pose: Matrix4f,
        a: Vec3,
        b: Vec3,
        thickness: Float,
        r: Float, g: Float, bl: Float, alpha: Float
    ) {
        val dir = b.subtract(a)
        val len = dir.length()
        if (len < 1e-6) return
        val dirN = dir.scale(1.0 / len)

        // a/b are camera-relative, so camera is origin
        val mid = a.add(b).scale(0.5)
        val view = mid.normalize()
        var side = dirN.cross(view)
        if (side.length() < 1e-6) {
            side = dirN.cross(Vec3(0.0, 1.0, 0.0))
            if (side.length() < 1e-6) side = dirN.cross(Vec3(1.0, 0.0, 0.0))
        }
        val sideN = side.normalize().scale(thickness.toDouble())

        val aL = a.subtract(sideN)
        val aR = a.add(sideN)
        val bL = b.subtract(sideN)
        val bR = b.add(sideN)

        vc.vertex(pose, aL.x.toFloat(), aL.y.toFloat(), aL.z.toFloat()).color(r, g, bl, alpha).uv(0f, 0f).endVertex()
        vc.vertex(pose, aR.x.toFloat(), aR.y.toFloat(), aR.z.toFloat()).color(r, g, bl, alpha).uv(1f, 0f).endVertex()
        vc.vertex(pose, bR.x.toFloat(), bR.y.toFloat(), bR.z.toFloat()).color(r, g, bl, alpha).uv(1f, 1f).endVertex()
        vc.vertex(pose, bL.x.toFloat(), bL.y.toFloat(), bL.z.toFloat()).color(r, g, bl, alpha).uv(0f, 1f).endVertex()
    }


    private fun safeNormalize(v: Vec3): Vec3 =
        if (v.lengthSqr() > 1e-12) v.normalize() else Vec3.ZERO

    private fun buildBasis(axisN: Vec3): Pair<Vec3, Vec3> {
        // Create orthonormal basis (U,V) perpendicular to axisN
        val ref = if (abs(axisN.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val u = safeNormalize(axisN.cross(ref))
        val v = safeNormalize(axisN.cross(u))
        return u to v
    }

    private fun buildBackbone(
        start: Vec3,
        end: Vec3,
        arcBias: ArcBias,
        segments: Int
    ): List<Vec3> {
        val out = ArrayList<Vec3>(segments + 1)
        val chord = end.subtract(start)

        for (i in 0..segments) {
            val t = i.toDouble() / segments.toDouble()
            var p = start.add(chord.scale(t))

            val envelope = 4.0 * t * (1.0 - t) // 0 at ends, 1 at center

            when (arcBias) {
                ArcBias.None -> {}

                is ArcBias.Parabola -> {
                    val dir = safeNormalize(arcBias.direction)
                    p = p.add(dir.scale(arcBias.strength * envelope))
                }

                is ArcBias.DoubleHump -> {
                    val dir = safeNormalize(arcBias.direction)
                    val wave = -sin(t * Math.PI * 2.0) // down-up-down if direction is "down"
                    p = p.add(dir.scale(arcBias.strength * wave * envelope))
                }

                is ArcBias.SineWave -> {
                    val dir = safeNormalize(arcBias.direction)
                    val wave = sin(t * Math.PI * 2.0 * arcBias.frequency)
                    p = p.add(dir.scale(arcBias.strength * wave * envelope))
                }

                is ArcBias.Spiral -> {
                    val axisN = safeNormalize(arcBias.axis ?: chord)
                    val (u, v) = buildBasis(axisN)

                    val theta = (t * arcBias.turns) * (Math.PI * 2.0) + arcBias.phase

                    // radius ramps from startMul to endMul along t, still enveloped to zero at endpoints
                    val ramp = arcBias.radiusStartMul + (arcBias.radiusEndMul - arcBias.radiusStartMul) * t
                    val r = arcBias.radius * ramp * envelope

                    val offset = u.scale(cos(theta) * r).add(v.scale(sin(theta) * r))
                    p = p.add(offset)
                }

                is ArcBias.DoubleHelix -> {
                    val axisN = safeNormalize(arcBias.axis ?: chord)
                    val (u, v) = buildBasis(axisN)

                    val theta = (t * arcBias.turns) * (Math.PI * 2.0) + arcBias.phase

                    // Two-strand feel: radius modulation twice per turn (cos 2θ).
                    // strandSeparation in [0..1] controls how obvious the “two strands” are.
                    val sep = arcBias.strandSeparation.coerceIn(0.0, 1.0)
                    val mod = 1.0 + sep * cos(2.0 * theta) // 2 lobes per rotation
                    val r = arcBias.radius * mod * envelope

                    val offset = u.scale(cos(theta) * r).add(v.scale(sin(theta) * r))
                    p = p.add(offset)
                }
            }

            out.add(p)
        }

        return out
    }

}
