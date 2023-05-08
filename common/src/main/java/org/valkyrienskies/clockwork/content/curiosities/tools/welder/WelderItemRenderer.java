package org.valkyrienskies.clockwork.content.curiosities.tools.welder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

public class WelderItemRenderer extends CustomRenderedItemModelRenderer<WelderModel> {
    @Override
    protected void render(ItemStack stack, WelderModel model, PartialItemModelRenderer renderer, ItemTransforms.TransformType transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

    }

    @Override
    public WelderModel createModel(BakedModel originalModel) {
        return new WelderModel(originalModel, "welder");
    }
}
