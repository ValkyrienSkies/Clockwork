package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.TileEntityRenderHelper;
import net.minecraft.core.Vec3i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.VSClientGameUtils;

@Mixin(TileEntityRenderHelper.class)
public abstract class MixinTileEntityRenderHelper {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.MixinTileEntityRenderHelper");

    @Redirect(
        method = "renderTileEntities(Lnet/minecraft/world/level/Level;Lcom/jozufozu/flywheel/core/virtual/VirtualRenderWorld;Ljava/lang/Iterable;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/jozufozu/flywheel/util/transform/TransformStack;translate(Lnet/minecraft/core/Vec3i;)Ljava/lang/Object;"
        )
    )
    private static Object redirectTranslate(final TransformStack instance, final Vec3i vec3i) {
        LOGGER.warn("redirTranslate " + vec3i);
        VSClientGameUtils.transformRenderIfInShipyard((PoseStack) instance, vec3i.getX(), vec3i.getY(), vec3i.getZ());
        return instance;
    }
}
