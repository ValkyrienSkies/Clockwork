package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.*

/** The same centreline is used for drawing, picking and riding a bind rope. */
class WandRopeCurve(val a: Vec3, val b: Vec3, length: Double) {
    private val delta = b.subtract(a)
    val sag = min(4.0, sqrt(max(0.0, length * length - delta.lengthSqr())) * 0.25)
    fun point(t: Double): Vec3 = a.lerp(b, t).add(0.0, -sin(t * PI) * sag, 0.0)
    fun derivative(t: Double): Vec3 = delta.add(0.0, -cos(t * PI) * PI * sag, 0.0)

    data class Hit(val parameter: Double, val distance: Double)
    fun pick(eye: Vec3, direction: Vec3, range: Double): Hit? {
        if (range <= 0 || delta.lengthSqr() < 0.01) return null
        val bounds = AABB(a, b).expandTowards(0.0, -sag, 0.0).inflate(0.22)
        if (!bounds.contains(eye) && bounds.clip(eye, eye.add(direction.scale(range))).isEmpty) return null
        val count = ceil((delta.length() + sag * 2) * 10).toInt().coerceIn(16, 2048)
        var best: Hit? = null
        for (i in 0..count) {
            val t = i.toDouble() / count
            val offset = point(t).subtract(eye)
            val distance = offset.dot(direction)
            if (distance !in 0.0..range || distance >= (best?.distance ?: Double.MAX_VALUE)) continue
            if (offset.subtract(direction.scale(distance)).lengthSqr() <= 0.22 * 0.22) best = Hit(t, distance)
        }
        return best
    }
}

object WandRopeSlidePhysics {
    const val MAX_SPEED = 0.7 // blocks per tick
    data class Step(val parameter: Double, val speed: Double, val finished: Boolean)

    fun step(curve: WandRopeCurve, parameter: Double, speed: Double, ticks: Double = 1.0): Step {
        val derivative = curve.derivative(parameter)
        val length = derivative.length()
        if (length < 0.01) return Step(parameter, 0.0, true)
        val nextSpeed = ((speed - derivative.y / length * 0.04 * ticks) * 0.995.pow(ticks)).coerceIn(-MAX_SPEED, MAX_SPEED)
        val next = parameter + nextSpeed * ticks / length
        return Step(next.coerceIn(0.0, 1.0), nextSpeed, next <= 0 && nextSpeed < 0 || next >= 1 && nextSpeed > 0)
    }

    /** Create's spring-follow approach, with feed-forward motion for moving ship anchors. */
    fun follow(position: Vec3, velocity: Vec3, target: Vec3, targetMotion: Vec3): Vec3 {
        val next = targetMotion.add(velocity.subtract(targetMotion).scale(0.65))
            .add(target.subtract(position).scale(0.25)).add(0.0, 0.08, 0.0)
        return if (next.lengthSqr() > 1.5 * 1.5) next.normalize().scale(1.5) else next
    }

    /** Short swept boxes cover the whole body between samples, including at full slide speed. */
    fun clearSweep(box: AABB, movement: Vec3, clear: (AABB) -> Boolean): Boolean {
        if (movement.lengthSqr() > 64 || !movement.lengthSqr().isFinite()) return false
        val steps = ceil(movement.length() / 0.2).toInt().coerceAtLeast(1)
        val part = movement.scale(1.0 / steps)
        return (0 until steps).all { i -> clear(box.move(part.scale(i.toDouble())).expandTowards(part).deflate(0.001)) }
    }
}
