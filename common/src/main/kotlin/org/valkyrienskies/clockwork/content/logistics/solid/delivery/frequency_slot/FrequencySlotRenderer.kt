package org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.utility.VecHelper
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.toShipRenderCoordinates


open class FrequencySlotRenderer<T : SmartBlockEntity>(context: BlockEntityRendererProvider.Context?): SmartBlockEntityRenderer<T>(
    context
) {

    override fun renderSafe(
        be: T,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)


        val cameraEntity = Minecraft.getInstance().cameraEntity
        val max = AllConfigs.client().filterItemRenderDistance.f

        if (!be.isVirtual && cameraEntity != null && distance(cameraEntity.position(), VecHelper.getCenterOf(be.blockPos)) > (max * max)) return

        val behaviour = be.getBehaviour(FrequencySlotBehaviour.TYPE)
            ?: return

        if (!behaviour.slot.shouldRender(be.blockState)) return
        val stack = behaviour.frequency.stack

        ms.pushPose()
        behaviour.slot.transform(be.blockState, ms)
        ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay)
        ms.popPose()

    }

    private fun distance(instance: Vec3, vec: Vec3): Double {
        val result = Minecraft.getInstance().level.toShipRenderCoordinates(vec, instance)
        return result.distanceToSqr(vec)
    }
}