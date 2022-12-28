package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import io.github.fabricators_of_create.porting_lib.block.CullingBlockEntityIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(CullingBlockEntityIterator.class)
public abstract class MixinCullingBlockEntityIterator {

    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.client.MixinCullingBlockEntityIterator");

    @Redirect(method = "nextCulled", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z")
    )
    private boolean inject(final Frustum instance, final AABB arg) {
        AABB newAABB = arg;
        final Level level = Minecraft.getInstance().level;
        if (level != null) {
            newAABB = VSGameUtilsKt.transformAabbToWorld(level, arg);
        }
        return instance.isVisible(newAABB);
    }
}
