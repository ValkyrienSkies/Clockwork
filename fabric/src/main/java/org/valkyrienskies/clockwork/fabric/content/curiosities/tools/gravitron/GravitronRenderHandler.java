package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.curiosities.zapper.ShootableGadgetRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.fabric.AllClockworkSounds;

public class GravitronRenderHandler extends ShootableGadgetRenderHandler {

    @Override
    protected void transformTool(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
        ms.translate(flip * -0.1f, 0.1f, -0.4f);
        ms.mulPose(Vector3f.YP.rotationDegrees(flip * 5.0F));
    }

    @Override
    protected void playSound(InteractionHand hand, Vec3 position) {
        float pitch = hand == InteractionHand.MAIN_HAND ? 0.1f : 0.9f;
        Minecraft mc = Minecraft.getInstance();
        AllClockworkSounds.PHYSICS_INFUSER_LIGHTNING.play(mc.level, mc.player, position, 0.1f, pitch);
    }

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return false;
    }
    @Override
    protected void transformHand(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {

    }
}
