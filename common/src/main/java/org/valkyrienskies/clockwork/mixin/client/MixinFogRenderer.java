package org.valkyrienskies.clockwork.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.util.AABBHelper;
import org.valkyrienskies.core.api.world.connectivity.SparseVoxelPosition;

import java.util.Set;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    /*
    private static Camera clockwork$capturedCamera = null;

    @Inject(method = "setupFog", at = @At(value = "HEAD"))
    private static void clockwork$submarineFogCapture(Camera activeRenderInfo, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean nearFog, float f, CallbackInfo ci){
        clockwork$capturedCamera = activeRenderInfo;
    }

    @WrapOperation(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V"))
    private static void clockwork$submarineFogStart(float f, Operation<Void> original){
        if (clockwork$capturedCamera != null && false) {
            original.call(f * 10f); //TODO air pocket sus size or something?
        } else {
           original.call(f);
        }
    }

    @WrapOperation(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V"))
    private static void clockwork$submarineFogEnd(float g, Operation<Void> original){
        if (clockwork$capturedCamera != null && false) {
            original.call(g * 10f); //TODO air pocket sus size or something?
        } else {
            original.call(g);
        }
    }

    @Unique
    private static void vs$scissorEach(Camera activeRenderInfo, FogRenderer.FogMode fogType, float farPlaneDistance, boolean nearFog, float f, Set<SparseVoxelPosition> voxels) {
        for (SparseVoxelPosition voxel : voxels) {
            var excludeAABB = AABBHelper.INSTANCE.toAABBic(voxel);
            if (excludeAABB != null) {
                int minX = (int) excludeAABB.minX;
                int minY = (int) excludeAABB.minY;
                int maxX = (int) excludeAABB.maxX;
                int maxY = (int) excludeAABB.maxY;

                // Adjust scissor box if needed (for example, convert coordinates to screen space)

                // Enable scissor test
                RenderSystem.enableScissor(minX, minY, maxX - minX, maxY - minY);
            }
        }
    }

    @Inject(method = "setupFog", at = @At(value = "HEAD"))
    private static void d(Camera activeRenderInfo, FogRenderer.FogMode fogType, float farPlaneDistance, boolean nearFog, float f, CallbackInfo ci){

        if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
            ClientLevel level = Minecraft.getInstance().level;


        }

        // Render the scene (fog will be applied, but excluded in the scissor box if specified)

        // Disable scissor test after rendering the scene

    }
    @Inject(method = "setupFog", at = @At(value = "TAIL"))
    private static void e(Camera activeRenderInfo, FogRenderer.FogMode fogType, float farPlaneDistance, boolean nearFog, float f, CallbackInfo ci){
        RenderSystem.disableScissor();
    }

*/
}
