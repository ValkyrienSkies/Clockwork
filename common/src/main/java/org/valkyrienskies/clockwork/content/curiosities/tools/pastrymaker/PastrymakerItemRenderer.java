package org.valkyrienskies.clockwork.content.curiosities.tools.pastrymaker;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockWorkHandlers;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.util.render.ClockworkCustomRenderedItemModel;

public class PastrymakerItemRenderer extends CustomRenderedItemModelRenderer<PastrymakerModel> {

    @Override
    protected void render(ItemStack stack, PastrymakerModel model, PartialItemModelRenderer renderer,
                          ItemTransforms.TransformType transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance()
                .getItemRenderer();
        renderer.render(model.getOriginalModel(), light);
        LocalPlayer player = Minecraft.getInstance().player;
        boolean mainHand = player.getMainHandItem() == stack;
        boolean offHand = player.getOffhandItem() == stack;
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;

        float offset = .5f / 16;
        float worldTime = AnimationTickHolder.getRenderTime() / 10;
        float angle = worldTime * -25;
        float speed = ClockWorkHandlers.PASTRYMAKER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded,
                AnimationTickHolder.getPartialTicks());
        float barrelOffset1 = (float) Math.sin(AnimationTickHolder.getPartialTicks() * 2) * speed;
        float barrelOffset2 = (float) Math.sin(-(AnimationTickHolder.getPartialTicks() * 2)) * speed;

        ms.translate(0, 0, barrelOffset1);
        renderer.render(model.getPartial("leftcannon"), light);

        ms.translate(0, 0, barrelOffset2);

        renderer.render(model.getPartial("rightcannon"), light);

        if (mainHand || offHand)
            angle += 360 * Mth.clamp(speed * 5, 0, 1);
        angle %= 360;



        ms.pushPose();
        ms.translate(0, offset, 0);
        ms.mulPose(Vector3f.ZP.rotationDegrees(angle));
        ms.translate(0, -offset, 0);
        renderer.render(model.getPartial("pressuretank"), light);





        ms.popPose();

        if (transformType == ItemTransforms.TransformType.GUI) {
            PastrymakerItem.getAmmoforPreview(stack)
                    .ifPresent(ammo -> {
                        PoseStack localMs = new PoseStack();
                        localMs.translate(-1 / 4f, -1 / 4f, 1);
                        localMs.scale(.5f, .5f, .5f);
                        TransformStack.cast(localMs)
                                .rotateY(-34);
                        itemRenderer.renderStatic(ammo, ItemTransforms.TransformType.GUI, light, OverlayTexture.NO_OVERLAY, localMs, buffer, 0);
                    });
        }

    }

    @Override
    public PastrymakerModel createModel(BakedModel originalModel) {
        return new PastrymakerModel(originalModel);
    }

}

