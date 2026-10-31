package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

internal object InfuserRibbon {
    data class Edge(val left: Vec3, val right: Vec3)

    fun edges(points: List<Vec3>, widths: List<Float>): List<Edge> {
        var previousSide = Vec3.ZERO
        return points.indices.map { i ->
            val tangent = points[minOf(i + 1, points.lastIndex)].subtract(points[maxOf(0, i - 1)]).normalize()
            var side = tangent.cross(points[i].normalize())
            if (side.lengthSqr() < 1e-8) side = if (previousSide.lengthSqr() > 0) previousSide else tangent.cross(Vec3(0.0, 1.0, 0.0))
            if (side.lengthSqr() < 1e-8) side = Vec3(1.0, 0.0, 0.0)
            side = side.normalize()
            if (side.dot(previousSide) < 0) side = side.scale(-1.0)
            previousSide = side
            val width = side.scale(widths[i].toDouble())
            Edge(points[i].subtract(width), points[i].add(width))
        }
    }

    fun draw(vc: VertexConsumer, matrix: Matrix4f, points: List<Vec3>, widths: List<Float>,
             alphas: List<Float>, r: Float, g: Float, b: Float) {
        if (points.size < 2) return
        val edges = edges(points, widths)
        fun vertex(pos: Vec3, u: Float, alpha: Float) {
            vc.vertex(matrix, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                .color(r, g, b, alpha.coerceIn(0f, 1f)).uv(u, 0.5f).endVertex()
        }
        for (i in 0 until edges.lastIndex) {
            vertex(edges[i].left, 0f, alphas[i]); vertex(edges[i].right, 1f, alphas[i])
            vertex(edges[i + 1].right, 1f, alphas[i + 1]); vertex(edges[i + 1].left, 0f, alphas[i + 1])
        }
    }
}
