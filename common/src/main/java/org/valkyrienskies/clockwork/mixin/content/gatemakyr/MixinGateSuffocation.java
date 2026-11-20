package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrSurfaceCollision;

@Mixin(LivingEntity.class)
public abstract class MixinGateSuffocation {
    @ModifyReturnValue(method = "isInWall", at = @At("RETURN"))
    private boolean clockwork$insideSolidBlock(boolean blocked) {
        return blocked && !GatemakyrSurfaceCollision.eyeInOpening((LivingEntity) (Object) this);
    }
}
