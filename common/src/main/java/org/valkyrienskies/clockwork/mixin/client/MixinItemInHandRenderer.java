package org.valkyrienskies.clockwork.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronEffects;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandHandEffects;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeRidingClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import com.mojang.math.Axis;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkItems;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @WrapMethod(method = "renderItem")
    private void clockwork$gravitronRenderOwner(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                               boolean leftHand, PoseStack matrices, MultiBufferSource buffers,
                                               int light, Operation<Void> original) {
        GravitronEffects.beginItem(entity, stack, context);
        WanderwandHandEffects.beginItem(entity, stack, context);
        InteractionHand ridingHand = entity instanceof Player player ? WandRopeRidingClient.ridingHand(player) : null;
        boolean raisedWrench = context.firstPerson() && ridingHand != null && entity.getItemInHand(ridingHand) == stack;
        if (raisedWrench) {
            matrices.pushPose();
            matrices.translate(0, 0.35, -0.08);
            matrices.mulPose(Axis.XP.rotationDegrees(-35));
        }
        try {
            original.call(entity, stack, context, leftHand, matrices, buffers, light);
        } finally {
            if (raisedWrench) matrices.popPose();
            GravitronEffects.endItem();
            WanderwandHandEffects.endItem();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void clockwork$gravitonCancelNbtUpdateAnim(CallbackInfo ci) {
        ItemStack newMainStack = minecraft.player.getMainHandItem();
        if (mainHandItem.getItem() == newMainStack.getItem()) {
            if (GravitronEffects.isGravitron(newMainStack) || newMainStack.getItem() instanceof WanderwandItem) {
                mainHandItem = newMainStack;
            }
        }

        ItemStack newOffStack = minecraft.player.getOffhandItem();

        if (offHandItem.getItem() == newOffStack.getItem()) {
            if (GravitronEffects.isGravitron(newOffStack)) {
                offHandItem = newOffStack;
            }
        }
    }
}
