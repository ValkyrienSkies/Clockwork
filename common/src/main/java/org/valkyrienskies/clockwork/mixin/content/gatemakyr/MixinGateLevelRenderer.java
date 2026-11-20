package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrShipPieces;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrEntitySlice;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Unique;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrRenderPass;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Mixin(LevelRenderer.class)
public abstract class MixinGateLevelRenderer implements GatemakyrRenderPass {
    @Unique private boolean clockwork$reloading;

    @WrapMethod(method = "allChanged")
    private void clockwork$reloadTerrainOnce(Operation<Void> reload) {
        if (!GatemakyrView.shaderCaptureSupported || GatemakyrView.getVanillaTerrain()) {
            reload.call();
            return;
        }
        if (clockwork$reloading) return;
        clockwork$reloading = true;
        try {
            reload.call();
        } finally {
            clockwork$reloading = false;
        }
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean clockwork$visibleAcrossGate(EntityRenderDispatcher dispatcher, Entity entity, Frustum frustum,
                                               double x, double y, double z, Operation<Boolean> original, PoseStack pose, float pt) {
        return original.call(dispatcher, entity, frustum, x, y, z) || GatemakyrEntitySlice.projectedVisible(entity, frustum, pt);
    }

    @WrapMethod(method = "renderEntity")
    private void clockwork$splitAtGate(Entity entity, double x, double y, double z, float pt, PoseStack pose,
                                       MultiBufferSource buffers, Operation<Void> draw) {
        MultiBufferSource sliced = GatemakyrEntitySlice.buffers(entity, x, y, z, pt, pose, buffers);
        if (sliced == null) return;
        var seat = GatemakyrShipPieces.seat(entity, true, pt);
        if (seat == null) {
            draw.call(entity, x, y, z, pt, pose, sliced);
            return;
        }
        Vec3 feet = seat.getFeet();
        pose.pushPose();
        try {
            pose.translate(feet.x - x, feet.y - y, feet.z - z);
            pose.mulPose(new Quaternionf(seat.getRotation()));
            pose.translate(x - Mth.lerp(pt, entity.xOld, entity.getX()), y - Mth.lerp(pt, entity.yOld, entity.getY()),
                z - Mth.lerp(pt, entity.zOld, entity.getZ()));
            draw.call(entity, x, y, z, pt, pose, sliced);
        } finally {
            pose.popPose();
        }
    }

    @Shadow private RenderTarget entityTarget;
    @Shadow private RenderTarget itemEntityTarget;
    @Shadow private RenderTarget weatherTarget;
    @Shadow private PostChain transparencyChain;

    @Override
    public void withGatemakyrTarget(Runnable draw) {
        RenderTarget outlines = entityTarget;
        RenderTarget items = itemEntityTarget;
        RenderTarget weather = weatherTarget;
        PostChain transparency = transparencyChain;
        try {
            entityTarget = null;
            itemEntityTarget = null;
            weatherTarget = null;
            transparencyChain = null;
            needsFrustumUpdate.set(true);
            draw.run();
        } finally {
            entityTarget = outlines;
            itemEntityTarget = items;
            weatherTarget = weather;
            transparencyChain = transparency;
            needsFrustumUpdate.set(true);
        }
    }
    @Shadow private ClientLevel level;
    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    @Shadow @Final private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    @Unique private final Map<ChunkPos, ViewArea> clockwork$remoteTerrain = new HashMap<>();
    @Unique private final Map<ChunkPos, Long> clockwork$lastView = new HashMap<>();

    @Inject(method = {"allChanged", "close"}, at = @At("HEAD"))
    private void clockwork$releaseRemoteTerrain(CallbackInfo ci) {
        clockwork$remoteTerrain.values().forEach(ViewArea::releaseAllBuffers);
        clockwork$remoteTerrain.clear();
        clockwork$lastView.clear();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void clockwork$changeLevel(ClientLevel next, CallbackInfo ci) {
        clockwork$releaseRemoteTerrain(ci);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void clockwork$expireTerrain(CallbackInfo ci) {
        if (clockwork$lastView.isEmpty()) return;
        long frame = GatemakyrView.currentFrame();
        clockwork$lastView.entrySet().removeIf(entry -> {
            if (frame - entry.getValue() <= 80) return false;
            clockwork$remoteTerrain.remove(entry.getKey()).releaseAllBuffers();
            return true;
        });
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void clockwork$updateRemoteSection(int x, int y, int z, boolean urgent, CallbackInfo ci) {
        clockwork$remoteTerrain.forEach((pos, area) -> {
            if (Math.abs(pos.x - x) <= 2 && Math.abs(pos.z - z) <= 2) area.setDirty(x, y, z, urgent);
        });
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;isChunkCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean clockwork$compiledGateChunk(LevelRenderer renderer, BlockPos pos, Operation<Boolean> original) {
        Vec3 center = GatemakyrView.remoteTerrainCenter;
        if (GatemakyrView.target == null || center == null || !GatemakyrView.getVanillaTerrain()) return original.call(renderer, pos);
        ViewArea area = clockwork$remoteTerrain.get(new ChunkPos(BlockPos.containing(center)));
        if (area == null) return original.call(renderer, pos);
        var section = area.getRenderChunkAt(pos);
        return section != null && section.compiled.get() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
    }

    @Shadow protected abstract void applyFrustum(Frustum frustum);
    @Shadow @Final private AtomicBoolean needsFrustumUpdate;

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void clockwork$gateVisibility(Camera camera, Frustum frustum, boolean captured, boolean spectator, CallbackInfo ci) {
        if (GatemakyrView.target != null && GatemakyrView.getVanillaTerrain()) {
            Vec3 center = GatemakyrView.remoteTerrainCenter;
            if (center == null) applyFrustum(frustum);
            else {
                ChunkPos exit = new ChunkPos(BlockPos.containing(center));
                ViewArea area = clockwork$remoteTerrain.get(exit);
                if (area == null) {
                    if (clockwork$remoteTerrain.size() >= 8) {
                        ChunkPos oldest = clockwork$lastView.entrySet().stream()
                            .min(Map.Entry.comparingByValue()).orElseThrow().getKey();
                        clockwork$lastView.remove(oldest);
                        area = clockwork$remoteTerrain.remove(oldest);
                    } else {
                        area = new ViewArea(chunkRenderDispatcher, level, 2, (LevelRenderer)(Object)this);
                    }
                    area.repositionCamera(exit.getMiddleBlockX(), exit.getMiddleBlockZ());
                    clockwork$remoteTerrain.put(exit, area);
                }
                clockwork$lastView.put(exit, GatemakyrView.currentFrame());
                renderChunksInFrustum.clear();
                for (var section : area.chunks) {
                    if (level.hasChunkAt(section.getOrigin()) && frustum.isVisible(section.getBoundingBox()))
                        renderChunksInFrustum.add(new LevelRenderer.RenderChunkInfo(section, null, 0));
                }
            }
            needsFrustumUpdate.set(true);
            ci.cancel();
        }
    }
}
