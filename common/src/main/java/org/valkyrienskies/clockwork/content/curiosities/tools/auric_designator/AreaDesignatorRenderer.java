package org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.ClockWorkMod;

public class AreaDesignatorRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel CRYSTAL = new PartialModel(ClockWorkMod.asResource("item/auric_designator/crystal"));
    protected static final PartialModel WAVE = new PartialModel(ClockWorkMod.asResource("item/auric_designator/wave"));

    private float crystalAngle = 0;

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemTransforms.TransformType transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!stack.is(ClockWorkItems.AURIC_DESIGNATOR.get())) {
            return;
        }
        AreaDesignatorItem adi = (AreaDesignatorItem) stack.getItem();

        TransformStack stacker = TransformStack.cast(ms);

        ms.pushPose();

        if (adi.animationType.equals(AreaDesignatorItem.Animation.DRAW)) {
            animateDraw(ms, stacker, buffer, light, overlay, adi.drawProgress, renderer);
        } else if (adi.animationType.equals(AreaDesignatorItem.Animation.SUCCESS)) {
            animateSuccess(ms, stacker, buffer, light, overlay, adi.successProgress, renderer);
        } else if (adi.animationType.equals(AreaDesignatorItem.Animation.DUMP)) {
            animateDump(ms, stacker, buffer, light, overlay, adi.dumpProgress, renderer);
        } else {
            renderer.renderSolid(model.getOriginalModel(), light);
            animateIdle(ms, stacker, buffer, light, overlay, renderer);
        }



        ms.popPose();
    }

    private void animateDraw(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {

    }
    private void animateSuccess(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {

    }
    private void animateDump(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {

    }
    private void animateIdle(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, PartialItemModelRenderer renderer) {
        ms.pushPose();
        float partialTicks = AnimationTickHolder.getPartialTicks();
        float heightAlt = Mth.sin(partialTicks/2);
        stacker.translateY(heightAlt * 0.1F);
        float nextCrystalAngle = Mth.clamp(crystalAngle + 1f, 0, 360);
        if (nextCrystalAngle == 360) {
            crystalAngle = 0;
        }
        stacker.rotateY(Mth.lerp(partialTicks, crystalAngle, nextCrystalAngle));
        crystalAngle = nextCrystalAngle;
        renderer.renderSolidGlowing(CRYSTAL.get(), light);
        stacker.translateY(-heightAlt * 0.1F);
        ms.popPose();
    }
}
