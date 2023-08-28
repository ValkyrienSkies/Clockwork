package org.valkyrienskies.clockwork.mixin.content.pulse;

import com.simibubi.create.foundation.outliner.ChasingAABBOutline;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChasingAABBOutline.class)
public interface ChasingAABBOutlineAccessor {
    @Accessor
    void setTargetBB(AABB bb);

    @Accessor
    AABB getTargetBB();
}
