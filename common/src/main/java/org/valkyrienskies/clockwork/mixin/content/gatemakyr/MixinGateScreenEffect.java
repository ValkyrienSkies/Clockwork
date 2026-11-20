package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrSurfaceCollision;

@Mixin(ScreenEffectRenderer.class)
public abstract class MixinGateScreenEffect {
    @WrapWithCondition(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private static boolean clockwork$visibleBacking(TextureAtlasSprite sprite, PoseStack pose) {
        var player = Minecraft.getInstance().player;
        return player == null || !GatemakyrSurfaceCollision.eyeInOpening(player);
    }
}
