package org.valkyrienskies.clockwork.renderer;

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
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.AreaDesignatorItem;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.util.EaseHelper;

import static org.valkyrienskies.clockwork.util.EaseHelper.*;

@SuppressWarnings("CannotAccess")
public class AreaDesignatorRenderer extends CustomRenderedItemModelRenderer {
    protected static final PartialModel CRYSTAL = new PartialModel(ClockworkMod.INSTANCE.asResource("item/auric_designator/crystal"));

    protected static final PartialModel POLE = new PartialModel(ClockworkMod.INSTANCE.asResource("item/auric_designator/pole"));

    protected static final PartialModel WAVE = new PartialModel(ClockworkMod.INSTANCE.asResource("item/auric_designator/wave"));

    private float crystalAngle = 0;

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemTransforms.TransformType transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!stack.is(ClockworkItems.AURIC_DESIGNATOR.get())) {
            return;
        }
        AreaDesignatorItem adi = (AreaDesignatorItem) stack.getItem();

        TransformStack stacker = TransformStack.cast(ms);

        ms.pushPose();

        renderer.renderSolid(model.getOriginalModel(), light);

        if (adi.getAnimationType().equals(AreaDesignatorItem.Animation.DRAW)) {
            animateDraw(ms, stacker, buffer, light, overlay, adi.getDrawProgress(), renderer);
        } else if (adi.getAnimationType().equals(AreaDesignatorItem.Animation.SUCCESS)) {
            animateSuccess(ms, stacker, buffer, light, overlay, adi.getSuccessProgress(), renderer);
        } else if (adi.getAnimationType().equals(AreaDesignatorItem.Animation.DUMP)) {
            animateDump(ms, stacker, buffer, light, overlay, adi.getDumpProgress(), renderer);
        } else {
            animateIdle(ms, stacker, buffer, light, overlay, adi.getIdleProgress(), renderer);
        }

        ms.popPose();
    }

    //todo : animations, for now they're all idle
    private void animateDraw(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {
        animateIdle(ms, stacker, buffer, light, overlay, progress, renderer);
    }
    private void animateSuccess(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {
        animateIdle(ms, stacker, buffer, light, overlay, progress, renderer);
    }
    private void animateDump(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {
        animateIdle(ms, stacker, buffer, light, overlay, progress, renderer);
    }
    private void animateIdle(PoseStack ms, TransformStack stacker, MultiBufferSource buffer, int light, int overlay, float progress, PartialItemModelRenderer renderer) {
        ms.pushPose();
        float partialTicks = AnimationTickHolder.getPartialTicks() - 1;
        float heightAlt = 3f/16f + (float) Math.sin(EaseHelper.INSTANCE.easeInOutSine(progress))/16f;
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
        ms.pushPose();
        stacker.translateY(heightAlt * 0.05F);
        renderer.renderSolid(POLE.get(), light);
        stacker.translateY(-heightAlt * 0.05F);
        ms.popPose();
    }
}