package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.sign

internal object GatemakyrClip {
    fun windowPlane(lens: Matrix4f, view: Matrix4f, offset: Vec3, normal: Vec3, width: Int, height: Int): Vector4f {
        val plane = Vector4f(normal.toVector3f(), -normal.dot(offset).toFloat())
        Matrix4f(lens).mul(view).invert().transpose().transform(plane)
        return Vector4f(2f * plane.x / width, 2f * plane.y / height, 2f * plane.z, plane.w - plane.x - plane.y - plane.z)
    }

    fun projection(lens: Matrix4f, view: Matrix4f, offset: Vec3, normal: Vec3): Matrix4f {
        val point = view.transformPosition(offset.toVector3f())
        val direction = view.transformDirection(normal.toVector3f()).normalize()
        val plane = Vector4f(direction, -direction.dot(point))
        val inverse = Matrix4f(lens).invert()
        val closest = inverse.transform(Vector4f(-sign(plane.x), -sign(plane.y), -1f, 1f))
        if (plane.dot(closest) >= 0f || plane.w >= -0.0001f) return Matrix4f(lens)
        val far = inverse.transform(Vector4f(sign(plane.x), sign(plane.y), 1f, 1f))
        val extent = plane.dot(far)
        if (extent <= 0.0001f) return Matrix4f(lens)
        return Matrix4f(lens).setRow(2, plane.mul(2f / extent).sub(lens.getRow(3, Vector4f())))
    }
}
