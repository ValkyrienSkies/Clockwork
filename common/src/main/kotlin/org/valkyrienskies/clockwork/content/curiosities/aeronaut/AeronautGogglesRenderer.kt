package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPartials

class AeronautGogglesRenderer: CustomRenderedItemModelRenderer() {
    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel,
        renderer: PartialItemModelRenderer,
        transformType: ItemDisplayContext,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val player = Minecraft.getInstance().player!!
        val partialTicks = AnimationTickHolder.getPartialTicks()

        if (transformType == ItemDisplayContext.HEAD) {
            ms.pushPose()

            renderer.renderSolid(ClockworkPartials.HAT_BASE.get(), light)
            val flapAngle = AeronautGogglesState.getFlapsAngle(player)
            val prevFlapAngle = AeronautGogglesState.getPrevFlapsAngle(player)
            val interpolatedFlapAngle = prevFlapAngle + (flapAngle - prevFlapAngle) * partialTicks
            ms.pushPose()
            val originLeft = fromModelSpace(2.725, 10.0, 8.0)
            ms.translate(-originLeft.x(), -originLeft.y(), -originLeft.z())
            ms.mulPose(org.joml.Quaternionf().setAngleAxis(interpolatedFlapAngle.toFloat(), 1f, 0f, 0f))
            ms.translate(originLeft.x(), originLeft.y(), originLeft.z())
            ms.pushPose()
            renderer.renderSolid(ClockworkPartials.HAT_FLAP_LEFT.get(), light)
            ms.popPose()
            ms.popPose()
            ms.pushPose()
            val originRight = fromModelSpace(13.275, 10.0, 8.0)
            ms.translate(-originRight.x(), -originRight.y(), -originRight.z())
            ms.mulPose(org.joml.Quaternionf().setAngleAxis(-interpolatedFlapAngle.toFloat(), 1f, 0f, 0f))
            ms.translate(originRight.x(), originRight.y(), originRight.z())
            ms.pushPose()
            renderer.renderSolid(ClockworkPartials.HAT_FLAP_RIGHT.get(), light)
            ms.popPose()
            ms.popPose()

            val wearingGoggles = AeronautGogglesState.getGogglesAreDown(player)
            ms.pushPose()
            ms.pushPose()
            if (!wearingGoggles) {
                //rotate so goggles are facing up
                ms.mulPose(org.joml.Quaternionf().setAngleAxis(-90f, 0f, 0f, 1f))
            }
            ms.popPose()
            renderer.renderSolid(ClockworkPartials.HAT_GOGGLES.get(), light)
            ms.popPose()

            ms.popPose()
        } else {
            renderer.renderSolid(model.originalModel, light)
        }
    }

    companion object {
        fun fromModelSpace(x: Double, y: Double, z: Double): Vector3dc {
            return Vector3d(x / 16.0, y / 16.0, z / 16.0)
        }

        fun toModelSpace(x: Double, y: Double, z: Double): Vector3dc {
            return Vector3d(x * 16.0, y * 16.0, z * 16.0)
        }

        fun Vector3dc.toModelSpace(destination: Vector3d = Vector3d()): Vector3d {
            return destination.set(this).mul(16.0)
        }

        fun Vector3dc.fromModelSpace(destination: Vector3d = Vector3d()): Vector3d {
            return destination.set(this).div(16.0)
        }
    }

}
