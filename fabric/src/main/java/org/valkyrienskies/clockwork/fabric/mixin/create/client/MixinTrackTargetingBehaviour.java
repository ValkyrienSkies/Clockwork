package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.trains.management.edgePoint.TrackTargetingBehaviour;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.VSClientGameUtils;

@Mixin(TrackTargetingBehaviour.class)
public class MixinTrackTargetingBehaviour {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.client.MixinTrackTargetingBehaviour");

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0)
    )
    private static void redirectTranslate(
        final PoseStack instance, final double pose, final double d, final double e) {
        VSClientGameUtils.transformRenderIfInShipyard(instance, pose, d, e);
    }
}
