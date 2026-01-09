package org.valkyrienskies.clockwork.util.beam

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.util.Mth.lerp
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.util.render.RenderUtil.hash64
import org.valkyrienskies.clockwork.util.render.RenderUtil.rand01

object BeamRenderer {
    private fun frac(x: Float): Float = x - kotlin.math.floor(x)

    private fun safePerpBasis(fwd: Vec3): Pair<Vec3, Vec3> {
        val ref = if (kotlin.math.abs(fwd.y) < 0.99) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val u = fwd.cross(ref)
        val uN = if (u.lengthSqr() < 1e-12) Vec3(1.0, 0.0, 0.0) else u.normalize()
        val v = fwd.cross(uN)
        val vN = if (v.lengthSqr() < 1e-12) Vec3(0.0, 0.0, 1.0) else v.normalize()
        return uN to vN
    }

    private fun smoothstep01(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

//    /** Continuous value noise in 1D: returns ~[-1, 1] */
//    private fun valueNoise1D(x: Float, seed: Long): Float {
//        val x0 = kotlin.math.floor(x).toInt()
//        val x1 = x0 + 1
//        val t = x - x0.toFloat()
//        val s = smoothstep01(t)
//
//        val v0 = hash64(seed xor (x0.toLong() * 0x9E3779B97F4A7C5L)) * 2f - 1f
//        val v1 = hash64(seed xor (x1.toLong() * 0x9E3779B97F4A7C5L)) * 2f - 1f
//        return lerp(v0, v1, s)
//    }
//
//    /** Fractal-ish noise (still smooth). Returns ~[-1, 1] */
//    private fun fbm1D(x: Float, seed: Long): Float {
//        var amp = 0.65f
//        var freq = 1.0f
//        var sum = 0f
//        var norm = 0f
//        for (o in 0 until 3) {
//            sum += valueNoise1D(x * freq, seed + o * 1013L) * amp
//            norm += amp
//            amp *= 0.5f
//            freq *= 2.0f
//        }
//        return sum / norm
//    }
//
//    /**
//     * @param t 0..1 along beam
//     * @param time seconds
//     * @param seed stable per-beam
//     */
//    private fun warbleMul(t: Float, time: Float, seed: Long): Float {
//        // Two traveling waves (always smooth)
//        val s0 = kotlin.math.sin((t * 9.0f + time * 2.8f) * (Math.PI * 2.0)).toFloat()
//        val s1 = kotlin.math.sin((t * 4.3f - time * 1.9f) * (Math.PI * 2.0)).toFloat()
//
//        // Smooth “chaos” riding on top (continuous!)
//        val n = fbm1D(t * 6.0f + time * 1.6f, seed)
//
//        // Mix
//        val wave = 0.45f * s0 + 0.25f * s1 + 0.55f * n
//
//        // Map to multiplier
//        val amp = 0.35f          // increase to ~0.5 for crazier
//        return (1f + wave * amp).coerceIn(0.55f, 1.65f)
//    }

    /**
     * Returns 0..1 jolt strength at `time`, with periodic random-ish jolts.
     * periodSeconds ~ how often a jolt *can* happen.
     * dutySeconds ~ how long the jolt lasts (soft edges).
     */
    private fun joltEnvelope(time: Float, seed: Long, periodSeconds: Float = 0.8f, dutySeconds: Float = 0.10f): Float {
        val k = kotlin.math.floor(time / periodSeconds).toInt()
        val tIn = time - k * periodSeconds

        // Decide if this period has a jolt
        val r = hash64(seed xor (k.toULong() * 0xD1B54A32D192ED03uL).toLong())
        val has = if (r > 0.65f) 1f else 0f // ~35% of periods jolt

        // Soft pulse shape in [0, dutySeconds]
        val x = (tIn / dutySeconds).coerceIn(0f, 1f)
        val attack = smoothstep01(x)                 // 0 -> 1
        val decay = smoothstep01(1f - x)             // 1 -> 0
        val pulse = attack * decay                   // bump

        return has * pulse
    }

    private fun shakeOffset(
        t: Float,
        time: Float,
        seed: Long,
        u: Vec3,
        v: Vec3,
        maxShake: Float = 0.06f // blocks
    ): Vec3 {
        val j = joltEnvelope(time, seed, periodSeconds = 0.85f, dutySeconds = 0.12f)
        if (j <= 0f) return Vec3.ZERO

        val ph0 = (time * 28.0f + t * 6.0f) * (Math.PI * 2.0f)
        val ph1 = (time * 33.0f + t * 9.0f + 1.3f) * (Math.PI * 2.0f)

        val sx = kotlin.math.sin(ph0.toDouble()).toFloat()
        val sy = kotlin.math.sin(ph1.toDouble()).toFloat()

        val amt = maxShake * j
        return u.scale((sx * amt).toDouble()).add(v.scale((sy * amt).toDouble()))
    }


    private fun warbleMulSinWithJolt(t: Float, time: Float, seed: Long): Float {
        // A traveling sine wave along the beam
        val spatialFreq = 8.0f         // ripples along beam
        val temporalFreq = 2.5f        // how fast ripples move
        val phase = (t * spatialFreq - time * temporalFreq) * (Math.PI * 2.0)
        val s = kotlin.math.sin(phase.toDouble()).toFloat()

        val baseAmp = 0.20f            // smooth warble strength
        var mul = 1f + s * baseAmp     // ~ [0.8 .. 1.2]

        // Occasional jolt makes it “kick”
        val j = joltEnvelope(time, seed, periodSeconds = 0.85f, dutySeconds = 0.12f)
        val joltAmp = 0.45f            // extra width during jolt
        mul += j * joltAmp

        return mul.coerceIn(0.55f, 1.75f)
    }


    fun renderBeamRibbon(
        poseStack: PoseStack,
        vc: VertexConsumer,
        camPosWorld: Vec3,
        blockOriginWorld: Vec3,
        startLocal: Vec3,
        endLocal: Vec3,
        width: Float,
        argb: Int,
        segments: Int,
        rampSteps: Int = 0,
        minWidthFrac: Float = 0.2f,
        tileWorldUnits: Float = 0.5f,    // <<< how long (in blocks) one 16px tile should represent
        scrollSpeed: Float = 1.5f,       // <<< tiles per second
        timeSeconds: Float = 0f
    ) {
        val dir = endLocal.subtract(startLocal)
        val len = dir.length()
        if (len < 1e-4) return
        val fwd = dir.normalize()

        val camLocal = camPosWorld.subtract(blockOriginWorld)
        val toCam = camLocal.subtract(startLocal)
        val toCamN = if (toCam.lengthSqr() > 1e-8) toCam.normalize() else Vec3(0.0, 0.0, 1.0)

        var right = fwd.cross(toCamN)
        if (right.lengthSqr() < 1e-8) right = fwd.cross(Vec3(0.0, 1.0, 0.0))
        if (right.lengthSqr() < 1e-8) right = fwd.cross(Vec3(1.0, 0.0, 0.0))
        right = right.normalize()

        val a0 = ((argb ushr 24) and 0xFF) / 255f
        val r0 = ((argb ushr 16) and 0xFF) / 255f
        val g0 = ((argb ushr 8)  and 0xFF) / 255f
        val b0 = ( argb         and 0xFF) / 255f

        val pose = poseStack.last().pose()

        fun widthAtSeg(segIndex: Int): Float {
            if (rampSteps <= 0) return width
            val t = (segIndex.toFloat() / rampSteps.toFloat()).coerceIn(0f, 1f)
            val s = t * t * (3f - 2f * t)
            return width * (minWidthFrac + (1f - minWidthFrac) * s)
        }

        // How many vertical tiles along the whole beam
        val tiles = (len.toFloat() / tileWorldUnits).coerceAtLeast(0.001f)
        val scroll = timeSeconds * scrollSpeed

        for (i in 0 until segments) {
            val t0 = i.toFloat() / segments.toFloat()
            val t1 = (i + 1).toFloat() / segments.toFloat()

            val p0 = startLocal.add(fwd.scale((len * t0).toDouble()))
            val p1 = startLocal.add(fwd.scale((len * t1).toDouble()))

            val fade0 = (1f - t0)
            val fade1 = (1f - t1)

            val half0 = widthAtSeg(i) * 0.5f
            val half1 = widthAtSeg(i + 1) * 0.5f

            val o0 = right.scale(half0.toDouble())
            val o1 = right.scale(half1.toDouble())

            // Manual repeat: keep V in [0,1) via frac
            val v0 = frac(t0 * tiles + scroll)
            val v1 = frac(t1 * tiles + scroll)

            fun emit(pos: Vec3, u: Float, v: Float, alphaMul: Float) {
                vc.vertex(pose, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                    .color(r0, g0, b0, a0 * alphaMul)
                    .uv(u, v)
                    .uv2(0xF000F0)
                    .endVertex()
            }

            emit(p0.subtract(o0), 0f, v0, fade0)
            emit(p0.add(o0),      1f, v0, fade0)
            emit(p1.add(o1),      1f, v1, fade1)
            emit(p1.subtract(o1), 0f, v1, fade1)
        }
    }


    fun renderBeamTube(
        poseStack: PoseStack,
        vc: VertexConsumer,
        camPosWorld: Vec3,
        blockOriginWorld: Vec3,
        startLocal: Vec3,
        endLocal: Vec3,
        width: Float,
        argb: Int,
        segments: Int,
        rampSteps: Int = 0,
        minWidthFrac: Float = 0.2f,
        ribbons: Int = 4,
        tileWorldUnits: Float = 0.5f,    // 1 texture tile per this many blocks
        scrollSpeed: Float = 2.0f,       // tiles per second
        timeSeconds: Float = 0f,
        uScale: Float = 1.0f,            // widen/squeeze texture across width
        seed: Long = 0L
    ) {
        val dir = endLocal.subtract(startLocal)
        val len = dir.length()
        if (len < 1e-4) return
        val fwd = dir.normalize()

        // Build a stable basis around fwd (u,v perpendicular)
        val ref = if (kotlin.math.abs(fwd.y) < 0.99) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val u = fwd.cross(ref).normalize()
        val v = fwd.cross(u).normalize()

        // We’ll render several ribbons, each using a different "right" direction around the tube
        for (k in 0 until ribbons) {
            val ang = (k.toDouble() / ribbons.toDouble()) * (Math.PI * 2.0)
            val right = u.scale(kotlin.math.cos(ang)).add(v.scale(kotlin.math.sin(ang))).normalize()

            renderBeamRibbonWithRight(
                poseStack = poseStack,
                vc = vc,
                startLocal = startLocal,
                endLocal = endLocal,
                rightUnit = right,
                width = width,
                argb = argb,
                segments = segments,
                rampSteps = rampSteps,
                minWidthFrac = minWidthFrac,
                tileWorldUnits = tileWorldUnits,
                scrollSpeed = scrollSpeed,
                timeSeconds = timeSeconds,
                uScale = uScale,
                seed = seed
            )
        }
    }

    fun renderBeamRibbonWithRight(
        poseStack: PoseStack,
        vc: VertexConsumer,
        startLocal: Vec3,
        endLocal: Vec3,
        rightUnit: Vec3,                 // must be normalized & perpendicular-ish to fwd
        width: Float,
        argb: Int,
        segments: Int,
        rampSteps: Int = 0,
        minWidthFrac: Float = 0.2f,
        tileWorldUnits: Float = 0.5f,    // 1 texture tile per this many blocks
        scrollSpeed: Float = 2.0f,       // tiles per second
        timeSeconds: Float = 0f,
        uScale: Float = 1.0f,            // widen/squeeze texture across width
        seed: Long = 0L
    ) {
        val dir = endLocal.subtract(startLocal)
        val len = dir.length()
        if (len < 1e-4) return
        val fwd = dir.normalize()

        // Ensure right is usable
        var right = rightUnit
        if (right.lengthSqr() < 1e-8) return
        // If right accidentally parallel, rebuild
        if (kotlin.math.abs(right.normalize().dot(fwd)) > 0.98) {
            val (u, _) = safePerpBasis(fwd)
            right = u
        }
        right = right.normalize()

        val a0 = ((argb ushr 24) and 0xFF) / 255f
        val r0 = ((argb ushr 16) and 0xFF) / 255f
        val g0 = ((argb ushr 8)  and 0xFF) / 255f
        val b0 = ( argb         and 0xFF) / 255f

        val pose = poseStack.last().pose()

        fun widthAtSeg(segIndex: Int): Float {
            if (rampSteps <= 0) return width
            val t = (segIndex.toFloat() / rampSteps.toFloat()).coerceIn(0f, 1f)
            val s = t * t * (3f - 2f * t) // smoothstep
            return width * (minWidthFrac + (1f - minWidthFrac) * s)
        }

        val tiles = (len.toFloat() / tileWorldUnits).coerceAtLeast(0.001f)
        val scroll = timeSeconds * scrollSpeed

        for (i in 0 until segments) {
            val t0 = i.toFloat() / segments.toFloat()
            val t1 = (i + 1).toFloat() / segments.toFloat()

            val p0 = startLocal.add(fwd.scale((len * t0).toDouble()))
            val p1 = startLocal.add(fwd.scale((len * t1).toDouble()))

            // Fade alpha along beam if you like
            val fade0 = 1f//(1f - t0)
            val fade1 = 1f//(1f - t1)

            val base0 = widthAtSeg(i)
            val base1 = widthAtSeg(i + 1)

            // Warble fades in after the first few segments so the emitter stays clean
            val warbleFade0 = smoothstep01((t0 - 0.05f) / 0.12f) // starts after 5% length
            val warbleFade1 = smoothstep01((t1 - 0.05f) / 0.12f)

            val w0 = base0 * (1f + (warbleMulSinWithJolt(t0, timeSeconds, seed) - 1f) * warbleFade0)
            val w1 = base1 * (1f + (warbleMulSinWithJolt(t1, timeSeconds, seed) - 1f) * warbleFade1)

            val half0 = w0 * 0.5f
            val half1 = w1 * 0.5f

            val o0 = right.scale(half0.toDouble())
            val o1 = right.scale(half1.toDouble())

            // V repeats (vertical tile) and scrolls
            val v0 = frac(t0 * tiles + scroll)
            val v1 = frac(t1 * tiles + scroll)

            // U across the ribbon. If uScale != 1, it tiles across width too.
            val u0 = 0f
            val u1 = uScale

            fun emit(pos: Vec3, u: Float, v: Float, alphaMul: Float) {
                vc.vertex(pose, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                    .color(r0, g0, b0, a0 * alphaMul)
                    .uv(u, v)
                    .uv2(0xF000F0) // fullbright
                    .endVertex()
            }

            emit(p0.subtract(o0), u0, v0, fade0)
            emit(p0.add(o0),      u1, v0, fade0)
            emit(p1.add(o1),      u1, v1, fade1)
            emit(p1.subtract(o1), u0, v1, fade1)
        }
    }




    fun renderImpactBurst(
        poseStack: PoseStack,
        vc: VertexConsumer,
        camPosWorld: Vec3,
        blockOriginWorld: Vec3,
        hitPosLocal: Vec3,
        normalWorld: Vec3,
        seed: Long,
        time: Float,
        rays: Int = 12,
        burstLen: Float = 0.8f,
        width: Float = 0.06f,
        argb: Int
    ) {
        val n = normalWorld.normalize()

        for (i in 0 until rays) {
            val z = rand01(i.toLong()) * 2.0 - 1.0
            val ang = rand01(i + 77L) * Math.PI * 2.0
            val rr = kotlin.math.sqrt(1.0 - z * z)
            var d = Vec3(rr * kotlin.math.cos(ang), z, rr * kotlin.math.sin(ang))

            if (d.dot(n) < 0.0) d = d.scale(-1.0)

            val len = burstLen * (0.35f + rand01(i + 999L).toFloat() * 0.65f) * (1f + 0.5f * kotlin.math.sin((time + rand01(i + 555L).toFloat()) * Math.PI))

            val endLocal = hitPosLocal.add(d.normalize().scale(len.toDouble()))

            renderBeamRibbon(
                poseStack = poseStack,
                vc = vc,
                camPosWorld = camPosWorld,
                blockOriginWorld = blockOriginWorld,
                startLocal = hitPosLocal,
                endLocal = endLocal,
                width = width,
                argb = argb,
                segments = 1,
                rampSteps = 0
            )
        }
    }

}
