package org.valkyrienskies.clockwork.mixin.content.pulse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.outliner.Outline;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Outliner.class)
public class MixinOutliner {

    @Final
    @Shadow
    private Map<Object, Outliner.OutlineEntry> outlines;

    /**
     * @author Potato
     * @reason Shader application
     */
    @Overwrite
    public void renderOutlines(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        outlines.forEach((key, entry) -> {
            Outline outline = entry.getOutline();
            Outline.OutlineParams params = outline.getParams();
            ((OutlineParamsAccessor) params).setAlpha(1);
            if (entry.isFading()) {
                int prevTicks = entry.getTicksTillRemoval() + 1;
                float fadeticks = Outliner.OutlineEntry.FADE_TICKS;
                float lastAlpha = prevTicks >= 0 ? 1 : 1 + (prevTicks / fadeticks);
                float currentAlpha = 1 + (entry.getTicksTillRemoval() / fadeticks);
                float alpha = Mth.lerp(pt, lastAlpha, currentAlpha);

                ((OutlineParamsAccessor)params).setAlpha(alpha * alpha * alpha);
                if (((OutlineParamsAccessor)params).getAlpha() < 1 / 8f)
                    return;
            }
            boolean shouldRenderShader = false;
            if (key instanceof String) {
                if (((String) key).contains("clusterID_")) {
                    shouldRenderShader = true;
                }
            }
            if (shouldRenderShader) {
                ms.pushPose();
                //shader time 3dtrol


                outline.render(ms, buffer, camera, pt);
                ms.popPose();
            } else {
                outline.render(ms, buffer, camera, pt);
            }
        });
    }
}
