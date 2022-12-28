package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.VSClientGameUtils;

@Mixin(targets = {"com.simibubi.create.foundation.utility.ghost.GhostBlockRenderer$DefaultGhostBlockRenderer",
    "com.simibubi.create.foundation.utility.ghost.GhostBlockRenderer$TransparentGhostBlockRenderer"})
public class MixinGhostBlockRenderer {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.client.MixinGhostBlockRenderer");

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0)
    )
    private void redirectTranslate(
        final PoseStack instance, final double pose, final double d, final double e) {
        VSClientGameUtils.transformRenderIfInShipyard(instance, pose, d, e);
    }
}
