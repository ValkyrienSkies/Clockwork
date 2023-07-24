package org.valkyrienskies.clockwork.mixin.content.pulse;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.EffectProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.valkyrienskies.clockwork.util.render.ClockworkGlslPreprocessor;

@Mixin(EffectProgram.class)
public class MixinEffectShader {
    @ModifyArg(method = "compileShader", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/EffectProgram;compileShaderInternal(Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;Ljava/io/InputStream;Ljava/lang/String;Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;)I"))
    private static GlslPreprocessor useCustomPreprocessor(GlslPreprocessor org) {
        return new ClockworkGlslPreprocessor();
    }
}
