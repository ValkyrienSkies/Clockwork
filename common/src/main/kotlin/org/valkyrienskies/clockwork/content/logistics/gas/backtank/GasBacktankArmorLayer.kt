package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.render.CachedBufferer
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

open class GasBacktankArmorLayer<T : LivingEntity?, M : EntityModel<T>?>(renderer: RenderLayerParent<T, M>?) : RenderLayer<T, M>(renderer!!) {

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
        if (entity!!.pose == Pose.SLEEPING) return
        GasBackTankItem.getWornBy(entity) ?: return
        val entityModel = parentModel
        if (entityModel !is HumanoidModel<*>) return

        val model = entityModel as HumanoidModel<*>
        val renderType = Sheets.cutoutBlockSheet()
        val renderedState = ClockworkBlocks.GAS_BACKTANK.defaultState
            //.setValue(BacktankBlock.HORIZONTAL_FACING, Direction.SOUTH)
        val backtank = CachedBufferer.block(renderedState)


        ms.pushPose()

        model.body.translateAndRotate(ms)
        ms.translate((-1 / 2f).toDouble(), (10 / 16f).toDouble(), 1.0)
        ms.scale(1f, -1f, -1f)

        backtank.forEntityRender()
            .light(light)
            .renderInto(ms, buffer.getBuffer(renderType))


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
            val layer: GasBacktankArmorLayer<*, *> = GasBacktankArmorLayer(entityRenderer)
            helper.register(layer)
        }


    }
}
