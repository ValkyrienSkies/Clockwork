package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrHit;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrRayPath;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrEntityInteraction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(GameRenderer.class)
public abstract class MixinGatePicking {
    @Inject(method = "pick", at = @At("TAIL"))
    private void clockwork$pickThroughGate(float pt, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getCameraEntity() != mc.player || mc.hitResult instanceof EntityHitResult) return;
        var hit = GatemakyrEntityInteraction.pick(mc.player, pt);
        if (hit != null) {
            mc.hitResult = hit;
            mc.crosshairPickEntity = hit.getEntity();
        }
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;shouldRenderBlockOutline()Z"))
    private boolean clockwork$selectOutlineView(boolean enabled) {
        return GatemakyrView.selectOutline(enabled);
    }

    @ModifyExpressionValue(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", ordinal = 0))
    private double clockwork$blockPathLength(double distance) {
        if (Minecraft.getInstance().hitResult instanceof GatemakyrHit hit && hit.getGatePath() != null) {
            double length = hit.getGatePath().getDistance();
            return length * length;
        }
        return distance;
    }

    @ModifyArg(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"), index = 5)
    private double clockwork$sourceEntityRange(double distance) {
        if (Minecraft.getInstance().hitResult instanceof GatemakyrHit hit) {
            GatemakyrRayPath path = hit.getGatePath();
            if (path != null) return Math.min(distance, path.getFirstGateDistance() * path.getFirstGateDistance());
        }
        return distance;
    }
}
