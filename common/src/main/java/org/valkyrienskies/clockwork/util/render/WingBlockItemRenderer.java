package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.ClockWorkPartials;

public class WingBlockItemRenderer extends CustomRenderedItemModelRenderer<WingModel> {
    @Override
    protected void render(ItemStack stack, WingModel model, PartialItemModelRenderer renderer, ItemTransforms.TransformType transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (transformType.firstPerson())
            renderFirstPerson(stack, buffer, ms, light);
        else
            renderInventory(stack, buffer, ms, light);
    }

    @Override
    public WingModel createModel(BakedModel originalModel) {
        return new WingModel(originalModel);
    }

    private void renderInventory(ItemStack stack, MultiBufferSource buffer, PoseStack ms, int light) {
        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
        SuperByteBuffer frame = CachedBufferer.partial(ClockWorkPartials.WING_FRAME_ITEM, ClockWorkBlocks.WING.getDefaultState()).light(light).translate(0, 0, -1);
        SuperByteBuffer sail = CachedBufferer.partial(ClockWorkPartials.WING_SAIL_ITEM, ClockWorkBlocks.WING.getDefaultState()).light(light).translate(0, 0, -1);

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
        SuperByteBuffer frame = CachedBufferer.partial(ClockWorkPartials.WING_FRAME_ITEM, ClockWorkBlocks.WING.getDefaultState())
                .light(light).translate(-0.5, -0.5, -0.5);
        SuperByteBuffer sail = CachedBufferer.partial(ClockWorkPartials.WING_SAIL_ITEM, ClockWorkBlocks.WING.getDefaultState())
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