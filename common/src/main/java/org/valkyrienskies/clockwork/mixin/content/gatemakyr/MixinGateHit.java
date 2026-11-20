package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrHit;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrRayPath;

@Mixin(BlockHitResult.class)
public abstract class MixinGateHit implements GatemakyrHit {
    @Unique private GatemakyrRayPath clockwork$path;

    @Override public GatemakyrRayPath getGatePath() { return clockwork$path; }
    @Override public void setGatePath(GatemakyrRayPath path) { clockwork$path = path; }
}
