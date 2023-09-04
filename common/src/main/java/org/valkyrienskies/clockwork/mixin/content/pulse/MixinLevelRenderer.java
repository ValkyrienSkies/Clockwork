package org.valkyrienskies.clockwork.mixin.content.pulse;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.outliner.*;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.primitives.*;
import org.joml.primitives.Intersectionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.client.render.scanner.ScannerRenderer;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.AreaDesignatorItem;
import org.valkyrienskies.clockwork.util.render.AreaDesignatorClusterRenderer;
import org.valkyrienskies.core.impl.util.VectorConversionsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow @Nullable private ClientLevel level;

    @Shadow @Final private Minecraft minecraft;

    /**
     * @deprecated Will be replaced with different shader soon, only here for temporary reference.
     */
    @Deprecated
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void renderScanner(final PoseStack poseStack, final float tickDelta, final long nanos, final boolean shouldRenderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        ScannerRenderer.Companion.getINSTANCE().doRender(poseStack);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void renderAreaDesignator(final PoseStack poseStack, final float tickDelta, final long nanos, final boolean shouldRenderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        ClockworkMod.INSTANCE.getOUTLINER().renderOutlines(poseStack, SuperRenderTypeBuffer.getInstance(), camera.getPosition(), tickDelta);
        AreaDesignatorClusterRenderer.Companion.getINSTANCE().renderDesignator(level, minecraft, poseStack, tickDelta, nanos, shouldRenderBlockOutline, camera, gameRenderer, lightTexture, projectionMatrix);
    }
}