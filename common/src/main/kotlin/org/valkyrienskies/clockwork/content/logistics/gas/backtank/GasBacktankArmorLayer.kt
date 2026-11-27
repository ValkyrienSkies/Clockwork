package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.mojang.blaze3d.vertex.PoseStack
import net.createmod.catnip.animation.AnimationTickHolder
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
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.level.block.HorizontalDirectionalBlock
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
        val backtank = CachedBuffers.block(renderedState)
        val wornStack = entity.getItemBySlot(EquipmentSlot.CHEST)
        val pressure = GasBacktankGauge.pressureFromItemStack(wornStack)
        val fill = GasBacktankGauge.fillFromPressure(pressure)
        val renderTime = AnimationTickHolder.getRenderTime(entity.level())
        val phase = (entity.id and 0xFF) * 0.1f
        val facing = renderedState.getValue(HorizontalDirectionalBlock.FACING)
        val vc = buffer.getBuffer(renderType)


        ms.pushPose()

        model.body.translateAndRotate(ms)
        ms.translate((-1 / 2f).toDouble(), (10 / 16f).toDouble(), 1.0)
        ms.scale(1f, -1f, -1f)

        backtank
            .light<SuperByteBuffer>(light)
            .renderInto(ms, vc)

        GasBacktankRenderer.renderNeedle(
            blockState = renderedState,
            facing = facing,
            fill = fill,
            pressure = pressure,
            renderTime = renderTime,
            phase = phase,
            ms = ms,
            vc = vc,
            light = light
        )


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
