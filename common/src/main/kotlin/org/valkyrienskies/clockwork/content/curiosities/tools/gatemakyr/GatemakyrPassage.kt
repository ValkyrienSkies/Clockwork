package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.valkyrienskies.mod.common.util.toJOML
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2

internal class GatemakyrPassage(val entrance: GatemakyrTarget.Frame, val exit: GatemakyrTarget.Frame) {
    fun direction(vector: Vec3): Vec3 {
        val u = vector.dot(entrance.u.normalize())
        val v = vector.dot(entrance.v.normalize())
        val n = vector.dot(entrance.normal.normalize())
        return exit.u.normalize().scale(-u).add(exit.v.normalize().scale(v)).subtract(exit.normal.normalize().scale(n))
    }

    fun rotation() = Quaterniond().lookAlong(
        direction(Vec3(0.0, 0.0, -1.0)).toJOML(), direction(Vec3(0.0, 1.0, 0.0)).toJOML()).conjugate()

    fun point(point: Vec3): Vec3 {
        val local = point.subtract(entrance.position)
        return exit.position.subtract(exit.u.scale(local.dot(entrance.u) / entrance.u.lengthSqr()))
            .add(exit.v.scale(local.dot(entrance.v) / entrance.v.lengthSqr()))
            .subtract(exit.normal.normalize().scale(local.dot(entrance.normal.normalize())))
    }

    fun velocity(vector: Vec3, eye: Vec3, previous: GatemakyrPassage?): Vec3 {
        if (previous == null) return direction(vector)
        fun previousPoint(now: GatemakyrTarget.Frame, old: GatemakyrTarget.Frame, world: Vec3): Vec3 {
            val local = world.subtract(now.position)
            return old.position.add(old.u.scale(local.dot(now.u) / now.u.lengthSqr()))
                .add(old.v.scale(local.dot(now.v) / now.v.lengthSqr()))
                .add(old.normal.normalize().scale(local.dot(now.normal.normalize())))
        }
        val entranceVelocity = eye.subtract(previousPoint(entrance, previous.entrance, eye))
        val destination = point(eye)
        val exitVelocity = destination.subtract(previousPoint(exit, previous.exit, destination))
        return direction(vector.subtract(entranceVelocity)).add(exitVelocity)
    }

    fun angles(pitch: Float, yaw: Float): Vec2 {
        val p = Math.toRadians(pitch.toDouble())
        val y = Math.toRadians(yaw.toDouble())
        val forward = direction(Vec3(-sin(y) * cos(p), -sin(p), cos(y) * cos(p)))
        val horizontal = forward.horizontalDistance()
        val heading = if (horizontal > 1e-6) atan2(-forward.x, forward.z) else {
            val left = direction(Vec3(cos(y), 0.0, sin(y)))
            atan2(left.z, left.x)
        }
        return Vec2(Math.toDegrees(atan2(-forward.y, horizontal)).toFloat(), Math.toDegrees(heading).toFloat())
    }

    fun cameraRoll(yaw: Float, rotated: Vec2): Float {
        val y = Math.toRadians(yaw.toDouble())
        val left = direction(Vec3(cos(y), 0.0, sin(y)))
        val pitch = Math.toRadians(rotated.x.toDouble())
        val heading = Math.toRadians(rotated.y.toDouble())
        val exitLeft = Vec3(cos(heading), 0.0, sin(heading))
        val exitUp = Vec3(-sin(heading) * sin(pitch), cos(pitch), cos(heading) * sin(pitch))
        return Math.toDegrees(atan2(left.dot(exitUp), left.dot(exitLeft))).toFloat()
    }

    companion object {
        fun radius(box: AABB, axis: Vec3): Double =
            (abs(axis.x) * box.xsize + abs(axis.y) * box.ysize + abs(axis.z) * box.zsize) * 0.5

        fun inset(box: AABB, frame: GatemakyrTarget.Frame, target: GatemakyrTarget): Vec3? {
            val offset = box.center.subtract(frame.position)
            fun correction(axis: Vec3, cells: Int): Vec3? {
                val unit = axis.normalize()
                val room = cells * axis.length() * 0.5 - radius(box, unit)
                if (room < -1e-7) return null
                val limit = room.coerceAtLeast(0.0)
                val center = offset.dot(unit)
                return unit.scale(center.coerceIn(-limit, limit) - center)
            }
            val u = correction(frame.u, target.width) ?: return null
            val v = correction(frame.v, target.height) ?: return null
            return u.add(v)
        }
    }
}
