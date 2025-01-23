package org.valkyrienskies.clockwork.util.render.outline

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.simibubi.create.foundation.outliner.AABBOutline
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class RotatedAABBOutline(aabb: AABB) : AABBOutline(aabb) {
    var rotationX: Float = 0f
    var rotationY: Float = 0f
    var rotationZ: Float = 0f

    override fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, pt: Float) {
        ms.pushPose()
        
        // Get the center of the AABB for rotation
        val center = bb.center
        
        // Translate to center, rotate, translate back
        ms.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z)
        
        // Apply rotations in ZYX order
        ms.mulPose(Quaternion(rotationZ, 0f, 0f, true))
        ms.mulPose(Quaternion(0f, rotationY, 0f, true))
        ms.mulPose(Quaternion(rotationX, 0f, 0f, true))
        
        ms.translate(-center.x + camera.x, -center.y + camera.y, -center.z + camera.z)
        
        // Call parent render method
        super.render(ms, buffer, camera, pt)
        
        ms.popPose()
    }
} 