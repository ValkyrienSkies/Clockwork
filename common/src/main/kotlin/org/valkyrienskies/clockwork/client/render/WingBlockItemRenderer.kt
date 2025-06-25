package org.valkyrienskies.clockwork.client.render

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.ItemTransform
import net.minecraft.client.renderer.block.model.ItemTransforms
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkPartials.WING_FRAME_ITEM
import org.valkyrienskies.clockwork.ClockworkPartials.WING_SAIL_ITEM

class WingBlockItemRenderer(var wingFrameItem: PartialModel) : CustomRenderedItemModelRenderer() {

    override fun render(
        stack: ItemStack,
        model: CustomRenderedItemModel?,
        renderer: PartialItemModelRenderer?,
        transformType: ItemDisplayContext,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (transformType.firstPerson()) {
            renderFirstPerson(stack, buffer, ms, light)
        } else {
            renderInventory(stack, buffer, ms, light)
        }
    }

    private fun renderInventory(stack: ItemStack, buffer: MultiBufferSource, ms: PoseStack, light: Int) {
        val vb = buffer.getBuffer(RenderType.cutout())
        val frame = CachedBufferer.partial(wingFrameItem, ClockworkBlocks.WING.defaultState).light(light)
            .translate(0.0, 0.0, -1.0)
        val sail = CachedBufferer.partial(WING_SAIL_ITEM, ClockworkBlocks.WING.defaultState).light(light)
            .translate(0.0, 0.0, -1.0)

        if (stack.hasTag()) {
            val tag = stack.getOrCreateTag()
            val color = tag.getInt("Clockwork\$color")
            sail.color(color)
        }

        frame.renderInto(ms, vb)
        sail.renderInto(ms, vb)
    }

    private fun renderFirstPerson(stack: ItemStack, buffer: MultiBufferSource, ms: PoseStack, light: Int) {
        val vb = buffer.getBuffer(RenderType.cutout())
        val frame = CachedBufferer.partial(wingFrameItem, ClockworkBlocks.WING.defaultState)
            .light(light).translate(-0.5, -0.5, -0.5)
        val sail = CachedBufferer.partial(WING_SAIL_ITEM, ClockworkBlocks.WING.defaultState)
            .light(light).translate(-0.5, -0.5, -0.5)

        if (stack.hasTag()) {
            val tag = stack.getOrCreateTag()
            val color = tag.getInt("Clockwork\$color")
            sail.color(color)
        }

        frame.renderInto(ms, vb)
        sail.renderInto(ms, vb)
    }
}