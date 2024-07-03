package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.datafixers.util.Pair
import com.simibubi.create.CreateClient
import com.simibubi.create.content.redstone.link.LinkBehaviour
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer
import com.simibubi.create.foundation.utility.Iterate
import com.simibubi.create.foundation.utility.Lang
import com.simibubi.create.foundation.utility.VecHelper
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3


object ChuteSlotRenderer {
    fun tick() {
        val mc = Minecraft.getInstance()
        val target = mc.hitResult
        if (target == null || target !is BlockHitResult) return

        val result = target
        val world = mc.level
        val pos = result.blockPos

        val behaviour = BlockEntityBehaviour.get(world, pos, DeliveryChuteBehavior.TYPE)
            ?: return


        val label: Component = Lang.translateDirect("logistics.firstFrequency")


        val bb = AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25)
        val hit = behaviour.testHit(target.getLocation())


        val box = ValueBox(label, bb, pos).passive(!hit)
        val empty = behaviour.frequency
            .stack
            .isEmpty

        if (!empty) box.wideOutline()

        CreateClient.OUTLINER.showValueBox(Pair.of(true, pos), box.transform(behaviour.slot))
            .highlightFace(result.direction)

        if (!hit) return

        val tip: MutableList<MutableComponent> = ArrayList()
        tip.add(label.copy())
        tip.add(
            Lang.translateDirect(if (empty) "logistics.filter.click_to_set" else "logistics.filter.click_to_replace")
        )
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip)

    }

    fun renderOnBlockEntity(
        be: SmartBlockEntity?, partialTicks: Float, ms: PoseStack,
        buffer: MultiBufferSource?, light: Int, overlay: Int
    ) {
        if (be == null || be.isRemoved) return

        val cameraEntity = Minecraft.getInstance().cameraEntity
        val max = AllConfigs.client().filterItemRenderDistance.f
        if (!be.isVirtual && cameraEntity != null && cameraEntity.position()
                .distanceToSqr(VecHelper.getCenterOf(be.blockPos)) > (max * max)
        ) return

        val behaviour = be.getBehaviour(DeliveryChuteBehavior.TYPE)
            ?: return


        val stack = behaviour.frequency.stack

        ms.pushPose()
        behaviour.slot.transform(be.blockState, ms)
        ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay)
        ms.popPose()

    }
}
