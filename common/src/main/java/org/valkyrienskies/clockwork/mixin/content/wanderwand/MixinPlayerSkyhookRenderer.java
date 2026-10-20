package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkModClient;
import net.minecraft.world.InteractionHand;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeRidingClient;

@Mixin(value = PlayerSkyhookRenderer.class, remap = false)
public abstract class MixinPlayerSkyhookRenderer {
    @Shadow private static void setHangingPose(boolean leftHand, HumanoidModel<?> model) { }

    @Inject(method = "afterSetupAnim", at = @At("TAIL"))
    private static void clockwork$bindSwing(Player player, HumanoidModel<?> model, CallbackInfo ci) {
        InteractionHand hand = WandRopeRidingClient.ridingHand(player);
        if (hand != null)
            setHangingPose((player.getMainArm() == HumanoidArm.LEFT) ^ (hand == InteractionHand.OFF_HAND), model);
        else if (ClockworkModClient.getWANDERWAND_EFFECT_RENDERER().isSwinging(player))
            setHangingPose(player.getMainArm() == HumanoidArm.LEFT, model);
    }
}
