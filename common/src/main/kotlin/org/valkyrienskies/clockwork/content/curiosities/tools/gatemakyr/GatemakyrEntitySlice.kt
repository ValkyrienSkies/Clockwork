package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector4f
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import kotlin.math.abs

object GatemakyrEntitySlice {
    private class Aperture(val gate: GatemakyrPlayerGate) {
        val frame = gate.passage.entrance
        val normal = frame.normal.normalize()
        private val u = frame.u.scale(gate.source.width * 0.5)
        private val v = frame.v.scale(gate.source.height * 0.5)
        val bounds = AABB(frame.position.add(u).add(v), frame.position.subtract(u).subtract(v))
            .minmax(AABB(frame.position.add(u).subtract(v), frame.position.subtract(u).add(v))).inflate(0.001)
    }

    private var snapshot: Map<*, *>? = null
    private var partialTick = -1f
    private var apertures = emptyList<Aperture>()

    private fun renderedBounds(entity: Entity, pt: Float): AABB {
        val t = 1.0 - pt
        return entity.boundingBox.move((entity.xOld - entity.x) * t, (entity.yOld - entity.y) * t, (entity.zOld - entity.z) * t)
    }

    private fun gate(entity: Entity, pt: Float): GatemakyrPlayerGate? {
        if (GatemakyrClient.rifts.size < 2 || !GatemakyrSurfaceCollision.eligible(entity.rootVehicle, entity is RemotePlayer)) return null
        val gates = GatemakyrClient.collisionGates()
        if (gates.isEmpty()) return null
        if (snapshot !== gates || partialTick != pt) {
            snapshot = gates
            partialTick = pt
            apertures = GatemakyrPlayerGate.snapshot(entity.level(), GatemakyrClient.rifts, true) { it.packet }
                .values.map(::Aperture)
        }
        val rendered = renderedBounds(entity, pt)
        val center = rendered.center
        var nearest = Double.POSITIVE_INFINITY
        var found: GatemakyrPlayerGate? = null
        for (aperture in apertures) {
            if (!aperture.bounds.intersects(rendered)) continue
            val distance = abs(center.subtract(aperture.frame.position).dot(aperture.normal))
            if (distance >= nearest || distance >= GatemakyrPassage.radius(rendered, aperture.normal) ||
                !aperture.gate.admits(rendered, center)) continue
            nearest = distance
            found = aperture.gate
        }
        return found
    }

    @JvmStatic fun projectedVisible(entity: Entity, frustum: Frustum, pt: Float): Boolean {
        val seat = GatemakyrShipPieces.seat(entity, true, pt)
        if (seat != null) return frustum.isVisible(entity.boundingBox.move(seat.feet.subtract(entity.position())).inflate(entity.bbHeight.toDouble()))
        val gate = gate(entity, pt) ?: return false
        val center = renderedBounds(entity, pt).center
        val radius = entity.boundingBox.run { xsize + ysize + zsize }
        return frustum.isVisible(AABB.ofSize(gate.passage.point(center), radius, radius, radius))
    }

    @JvmStatic fun buffers(entity: Entity, x: Double, y: Double, z: Double, pt: Float, pose: PoseStack,
                           original: MultiBufferSource): MultiBufferSource? {
        val selfView = GatemakyrView.camera?.entity === entity && Minecraft.getInstance().options.cameraType.isFirstPerson &&
            !ShaderPackCompat.shadowPass()
        val viewer = if (selfView) renderedBounds(entity, pt).inflate(0.2) else null
        if (viewer?.contains(x, y, z) == true) return null
        val gate = gate(entity, pt) ?: return original
        val passage = gate.passage
        val entry = passage.entrance
        val camera = Vec3(x, y, z)
        val normal = entry.normal.normalize()
        val center = renderedBounds(entity, pt).center
        val signed = center.subtract(entry.position).dot(normal)
        val side = if (!gate.source.surface && !gate.destination.surface && signed < 0.0) -1.0 else 1.0
        val planePoint = center.subtract(normal.scale(signed))
        val view = pose.last().pose()
        val point = view.transformPosition(planePoint.subtract(camera).toVector3f())
        val axis = Matrix3f(view).normal().transform(normal.scale(side).toVector3f()).normalize()
        val plane = Vector4f(axis, -axis.dot(point))
        val rotation = Matrix4f().setColumn(0, Vector4f(passage.direction(Vec3(1.0, 0.0, 0.0)).toVector3f(), 0f))
            .setColumn(1, Vector4f(passage.direction(Vec3(0.0, 1.0, 0.0)).toVector3f(), 0f))
            .setColumn(2, Vector4f(passage.direction(Vec3(0.0, 0.0, 1.0)).toVector3f(), 0f))
        val mappedPlanePoint = passage.point(planePoint)
        val projectBack = viewer == null || !viewer.contains(planePoint.add(
            GatemakyrPassage(passage.exit, entry).direction(camera.subtract(mappedPlanePoint))))
        val transform = Matrix4f(view).translate(mappedPlanePoint.subtract(camera).toVector3f())
            .mul(rotation).translate(camera.subtract(planePoint).toVector3f()).mul(Matrix4f(view).invert())
        return MultiBufferSource { type ->
            val out = original.getBuffer(type)
            when (type.mode()) {
                VertexFormat.Mode.QUADS -> GateSliceVertices(out, plane, transform, 4, projectBack)
                VertexFormat.Mode.TRIANGLES -> GateSliceVertices(out, plane, transform, 3, projectBack)
                else -> out
            }
        }
    }
}
