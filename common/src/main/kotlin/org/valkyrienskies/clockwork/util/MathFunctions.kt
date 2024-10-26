package org.valkyrienskies.clockwork.util

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.lwjgl.system.CallbackI.Z




object MathFunctions {
    fun removeAxis(axis: Direction.Axis, vec: Vec3): Vector2f {
        val out: Vector2f
        when (axis) {
            Direction.Axis.X -> out = Vector2f(vec.y.toFloat(), vec.z.toFloat())
            Direction.Axis.Y -> out = Vector2f(vec.x.toFloat(), vec.z.toFloat())
            Direction.Axis.Z -> out = Vector2f(vec.x.toFloat(), vec.y.toFloat())
            else -> throw IllegalStateException("Unexpected value: $axis")
        }
        return out
    }

    fun Vector2f.isWithin(x1: Float, y1: Float, difference: Float): Boolean {
        return Math.abs(x1 - this.x) < difference && Math.abs(y1 - this.y) < difference
    }
}