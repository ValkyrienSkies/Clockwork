package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.mojang.blaze3d.vertex.PoseStack
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesRenderer.Companion.fromModelSpace
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.IAeronautEquipment.Companion.isWearingAnyAeronaut

open class AeronautArmorLayer<T : LivingEntity, M : EntityModel<T>?>(renderer: RenderLayerParent<T, M>?) : RenderLayer<T, M>(renderer!!) {

    override fun render(
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        if (!entity.isWearingAnyAeronaut()) return
        val entityModel = parentModel
        if (entityModel !is HumanoidModel<*>) return

        val model = entityModel as HumanoidModel<*>
        val renderType = Sheets.cutoutBlockSheet()

        val player = Minecraft.getInstance().player!!
        val partialTicks = AnimationTickHolder.getPartialTicks()

        ms.pushPose()

        val hatBase = ClockworkPartials.HAT_BASE
        val air = Blocks.AIR.defaultBlockState()

        CachedBuffers.partial(hatBase, air)
            .disableDiffuse<SuperByteBuffer?>()
            .light<SuperByteBuffer?>(light)
            .renderInto(ms, buffer.getBuffer(renderType))

        val flapAngle = AeronautGogglesState.getFlapsAngle(player)
        val prevFlapAngle = AeronautGogglesState.getPrevFlapsAngle(player)
        val interpolatedFlapAngle = prevFlapAngle + (flapAngle - prevFlapAngle) * partialTicks
        ms.pushPose()
        val originLeft = fromModelSpace(2.725, 10.0, 8.0)
        ms.translate(-originLeft.x(), -originLeft.y(), -originLeft.z())
        ms.mulPose(org.joml.Quaternionf().setAngleAxis(interpolatedFlapAngle.toFloat(), 1f, 0f, 0f))
        ms.translate(originLeft.x(), originLeft.y(), originLeft.z())
        ms.pushPose()

        val hatFlapLeft = ClockworkPartials.HAT_FLAP_LEFT
        CachedBuffers.partial(hatFlapLeft, air)
            .disableDiffuse<SuperByteBuffer?>()
            .light<SuperByteBuffer?>(light)
            .renderInto(ms, buffer.getBuffer(renderType))
        ms.popPose()
        ms.popPose()
        ms.pushPose()
        val originRight = fromModelSpace(13.275, 10.0, 8.0)
        ms.translate(-originRight.x(), -originRight.y(), -originRight.z())
        ms.mulPose(org.joml.Quaternionf().setAngleAxis(-interpolatedFlapAngle.toFloat(), 1f, 0f, 0f))
        ms.translate(originRight.x(), originRight.y(), originRight.z())
        ms.pushPose()

        val hatFlapRight = ClockworkPartials.HAT_FLAP_RIGHT
        CachedBuffers.partial(hatFlapRight, air)
            .disableDiffuse<SuperByteBuffer?>()
            .light<SuperByteBuffer?>(light)
            .renderInto(ms, buffer.getBuffer(renderType))
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

        val hatGoggles = ClockworkPartials.HAT_GOGGLES
        CachedBuffers.partial(hatGoggles, air)
            .disableDiffuse<SuperByteBuffer?>()
            .light<SuperByteBuffer?>(light)
            .renderInto(ms, buffer.getBuffer(renderType))
        ms.popPose()

        ms.popPose()
    }

    companion object {

        @JvmStatic
        fun registerOn(
            entityRenderer: EntityRenderer<*>?,
            helper: LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper
        ) {
            if (entityRenderer !is LivingEntityRenderer<*, *>) return
            if (entityRenderer.model !is HumanoidModel<*>) return
            val layer: AeronautArmorLayer<*, *> = AeronautArmorLayer(entityRenderer)
            helper.register(layer)
        }


    }
}
