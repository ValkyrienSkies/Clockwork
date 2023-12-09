package org.valkyrienskies.clockwork.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkPartials;

public class WingBlockItemRenderer extends CustomRenderedItemModelRenderer {
    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (transformType.firstPerson())
            renderFirstPerson(stack, buffer, ms, light);
        else
            renderInventory(stack, buffer, ms, light);
    }

    private void renderInventory(ItemStack stack, MultiBufferSource buffer, PoseStack ms, int light) {
        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
        SuperByteBuffer frame = CachedBufferer.partial(ClockworkPartials.INSTANCE.getWING_FRAME_ITEM(), ClockworkBlocks.WING.getDefaultState()).light(light).translate(0, 0, -1);
        SuperByteBuffer sail = CachedBufferer.partial(ClockworkPartials.INSTANCE.getWING_SAIL_ITEM(), ClockworkBlocks.WING.getDefaultState()).light(light).translate(0, 0, -1);

        if (stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTag();
            int color = tag.getInt("Clockwork$color");
            sail.color(color);
        }

        frame.renderInto(ms, vb);
        sail.renderInto(ms, vb);
    }

    private void renderFirstPerson(ItemStack stack, MultiBufferSource buffer, PoseStack ms, int light) {
        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
        SuperByteBuffer frame = CachedBufferer.partial(ClockworkPartials.INSTANCE.getWING_FRAME_ITEM(), ClockworkBlocks.WING.getDefaultState())
                .light(light).translate(-0.5, -0.5, -0.5);
        SuperByteBuffer sail = CachedBufferer.partial(ClockworkPartials.INSTANCE.getWING_SAIL_ITEM(), ClockworkBlocks.WING.getDefaultState())
                .light(light).translate(-0.5, -0.5, -0.5);

        if (stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTag();
            int color = tag.getInt("Clockwork$color");
            sail.color(color);
        }

        frame.renderInto(ms, vb);
        sail.renderInto(ms, vb);
    }
}