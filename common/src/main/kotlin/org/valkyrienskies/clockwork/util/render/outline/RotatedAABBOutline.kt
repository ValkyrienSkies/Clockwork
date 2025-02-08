package org.valkyrienskies.clockwork.util.render.outline

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.outliner.AABBOutline
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3dc
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.clockwork.util.*

class RotatedAABBOutline(aabb: AABB, var directon: Vector3dc) : AABBOutline(aabb) {

    override fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, pt: Float) {
        ms.pushPose()

        val cx = bb.minX
        val cy = bb.minY
        val cz = bb.minZ

        // Translate to center, rotate, translate back
        ms.translate(cx - camera.x, cy - camera.y, cz - camera.z)

        ms.mulPose(getHingeRotation(directon).toMinecraft())

        ms.translate(-cx + camera.x, -cy + camera.y, -cz + camera.z)

        // Call parent render method
        super.render(ms, buffer, camera, pt)
        
        ms.popPose()
    }
} 