package org.valkyrienskies.clockwork.mixin.content.pulse;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.outliner.Outline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(Outline.OutlineParams.class)
public interface OutlineParamsAccessor {
    @Accessor("alpha")
    float getAlpha();

    @Accessor("alpha")
    void setAlpha(float alpha);

    @Accessor
    boolean getDisableCull();

    @Accessor
    Optional<AllSpecialTextures> getFaceTexture();
}
