package org.valkyrienskies.clockwork.content.logistics.solid.delivery

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.redstone.link.LinkRenderer
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.utility.VecHelper
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getShipManagingPos

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

        if (ms==null) return
        if (be == null || be.isRemoved) return

        val cameraEntity = Minecraft.getInstance().cameraEntity
        val max = AllConfigs.client().filterItemRenderDistance.f

        val ship = be.level.getShipManagingPos(be.blockPos)

        val pos: Vec3
        if (ship!=null) {
            val sPos = ship.shipToWorld.transformPosition(Vector3d(be.blockPos.x.toDouble(), be.blockPos.y.toDouble(), be.blockPos.z.toDouble()))
            pos = Vec3(sPos.x,sPos.y,sPos.z)
        } else pos = VecHelper.getCenterOf(be.blockPos)

        if (!be.isVirtual && cameraEntity != null && cameraEntity.position().distanceToSqr(pos) > (max * max)) return

        val behaviour = be.getBehaviour(FrequencySlotBehaviour.TYPE)
            ?: return

        if (!behaviour.slot.shouldRender(be.blockState)) return
        val stack = behaviour.frequency.stack

        ms.pushPose()
        behaviour.slot.transform(be.blockState, ms)
        ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay)
        ms.popPose()

    }
}