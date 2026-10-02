package org.valkyrienskies.clockwork.mixin.content.gravitron;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronAnimation;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronEffects;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandHandEffects;
import net.minecraft.world.entity.HumanoidArm;

@Mixin(HumanoidModel.class)
public class MixinHumanoidModel {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void clockwork$gravitronWeight(LivingEntity entity, float walk, float walkAmount,
                                          float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        var wandState = WanderwandHandEffects.INSTANCE.state(player);
        if (wandState != null) {
            var wandPose = wandState.getAnimation().sample(player.level().getGameTime(), Minecraft.getInstance().getFrameTime());
            ModelPart arm = player.getMainArm() == HumanoidArm.RIGHT ? rightArm : leftArm;
            arm.xRot -= 0.35f - wandPose.getLower() * 0.4f + wandPose.getTilt() * 0.012f;
            arm.zRot += wandPose.getRoll() * 0.009f;
        }
        if (!GravitronEffects.isGravitron(player.getMainHandItem())) return;
        GravitronEffects.State state = GravitronEffects.INSTANCE.state(player);
        if (state == null) return;
        GravitronAnimation.Pose pose = state.getAnimation().sample(player.level().getGameTime(), Minecraft.getInstance().getFrameTime());
        float weight = pose.getLowering() * 0.45f - pose.getRecoil() * 0.22f;
        rightArm.xRot += weight;
        leftArm.xRot += weight;
    }
}
