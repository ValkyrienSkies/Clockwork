package org.valkyrienskies.clockwork.forge.mixin.create.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.trains.management.edgePoint.TrackTargetingBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.VSClientGameUtils;

@Mixin(TrackTargetingBehaviour.class)
public class MixinTrackTargetingBehaviour {
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0)
    )
    private static void redirectTranslate(
        final PoseStack instance, final double pose, final double d, final double e) {
        VSClientGameUtils.transformRenderIfInShipyard(instance, pose, d, e);
    }
}