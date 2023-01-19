package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkHandlers;
import org.valkyrienskies.clockwork.ClockWorkMod;

public class GravitronItemRenderer extends CustomRenderedItemModelRenderer<GravitronModel> {

    @Override
    protected void render(ItemStack stack, GravitronModel model, PartialItemModelRenderer renderer, ItemTransforms.TransformType transformType,
                          PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

        float pt = AnimationTickHolder.getPartialTicks();
        float worldTime = AnimationTickHolder.getRenderTime() / 20;

        renderer.renderSolid(model.getOriginalModel(), light);

        LocalPlayer player = Minecraft.getInstance().player;
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        boolean mainHand = player.getMainHandItem() == stack;
        boolean offHand = player.getOffhandItem() == stack;
        float animation = getAnimationProgress(pt, leftHanded, mainHand);

        float angle = worldTime * -25;
        if (mainHand || offHand)
            angle += 360 * animation;
        Vec3 offset = new Vec3(7.7016, 8.3536, 1.6978);
        renderer.render(model.getPartial("dialhand"), light);

        renderer.render(model.getPartial("prongleftone"), light);
        renderer.render(model.getPartial("prongrightone"), light);
        renderer.render(model.getPartial("pronglefttwo"), light);
        renderer.render(model.getPartial("prongrighttwo"), light);

        renderer.render(model.getPartial("prongleftthree"), light);
        renderer.render(model.getPartial("prongrightthree"), light);

        ms.mulPose(Vector3f.XP.rotationDegrees(45f));
        ms.translate(0, -0.300, 0.125);

        renderer.render(model.getPartial("prongtopone"), light);

        ms.mulPose(Vector3f.XP.rotationDegrees(-15f));
        ms.translate(0, 0.125, 0.025);
        renderer.render(model.getPartial("prongtoptwo"), light);
        renderer.render(model.getPartial("prongtopthree"), light);

    }

    protected float getAnimationProgress(float pt, boolean leftHanded, boolean mainHand) {
        float animation = ClockWorkHandlers.GRAVITRON_HANDLER.getAnimation(mainHand ^ leftHanded, pt);
        return Mth.clamp(animation * 5, 0, 1);
    }

    protected void renderDial() {

    }

    protected void renderProngs() {

    }

    @Override
    public GravitronModel createModel(BakedModel originalModel) {
        return new GravitronModel(originalModel);
    }

}