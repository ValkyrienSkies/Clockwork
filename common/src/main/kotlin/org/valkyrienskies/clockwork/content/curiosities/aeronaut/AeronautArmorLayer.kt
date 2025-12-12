package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.mojang.blaze3d.vertex.PoseStack
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import org.valkyrienskies.clockwork.ClockworkBlocks
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
