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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.client.render.scanner.ScannerRenderer;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorItem;
import org.valkyrienskies.core.impl.util.VectorConversionsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow @Nullable private ClientLevel level;

    @Unique
    private Set<AABBic> hoveredCluster = new HashSet<>();

    @Unique
    private HashMap<Set<AABBic>,BlockClusterOutline> clusterOutlines = new HashMap<>();

    @Unique
    private static Color HOVERPURPLE = new Color(238,130,238);

    @Unique
    private static Color IDLEPURPLE = new Color(221,160,221);

    @Unique
    private ChasingAABBOutline selectionBox = new ChasingAABBOutline(new AABB(0,0,0,0,0,0));

    /**
     * @deprecated Will be replaced with different shader soon, only here for temporary reference.
     */
    @Deprecated
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void renderScanner(final PoseStack poseStack, final float tickDelta, final long nanos, final boolean shouldRenderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        ScannerRenderer.INSTANCE.doRender(poseStack);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.BEFORE))
    private void renderAreaDesignator(final PoseStack poseStack, final float tickDelta, final long nanos, final boolean shouldRenderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        if (level != null) {
            for (Player player : level.players()) {
                if (player.getMainHandItem().is(ClockWorkItems.AURIC_DESIGNATOR.get())) {
                    //other players
                    AreaDesignatorItem adi = (AreaDesignatorItem) player.getMainHandItem().getItem();

                    Set<Set<AABBic>> clusters = adi.selectionClusters;

                    for (Set<AABBic> cluster : clusters) {
                        if (clusterOutlines.containsKey(cluster)) {
                            continue;
                        }
                        BlockClusterOutline clusterOutline = new BlockClusterOutline(adi.blocksFromCluster(cluster));
                        clusterOutline.getParams().withFaceTexture(AllSpecialTextures.SELECTION).colored(IDLEPURPLE);
                        clusterOutlines.put(cluster, clusterOutline);
                    }

                    //local player
                    if (player.isLocalPlayer()) {
                        Minecraft mc = Minecraft.getInstance();
                        LocalPlayer localPlayer = mc.player;
                        if (localPlayer == null) {
                            return;
                        }
                        BlockPos hovered = null;
                        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                            hovered = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                        }
                        Vector3ic hoveredBlockPos = new Vector3i();
                        if (hovered != null) {
                            hoveredBlockPos = VectorConversionsMCKt.toJOML(hovered);
                        }


                        // find existing hovered cluster if existing
                        boolean foundCluster = false;

                        for (Set<AABBic> cluster : clusters) {
                            double range = 10;
                            Vector3dc tempOrigin = VectorConversionsMCKt.toJOML(RaycastHelper.getTraceOrigin(localPlayer));
                            Vector3dc tempTarget = VectorConversionsMCKt.toJOML(RaycastHelper.getTraceTarget(localPlayer, range, RaycastHelper.getTraceOrigin(localPlayer)));
                            Vector3fc traceOrigin = new Vector3f((float) tempOrigin.x(), (float) tempOrigin.y(), (float) tempOrigin.z());
                            Vector3fc traceTarget = new Vector3f((float) tempTarget.x(), (float) tempTarget.y(), (float) tempTarget.z());
                            LineSegmentf cast = new LineSegmentf(traceOrigin, traceTarget);
                            for (AABBic box : cluster) {
                                int intersection = Intersectionf.intersectLineSegmentAab(cast, new AABBi(box), new Vector2f());
                                if (intersection != Intersectionf.OUTSIDE) {
                                    hoveredCluster = cluster;
                                    foundCluster = true;
                                    break;
                                }
                            }
                        }
                        if (!foundCluster) {
                            hoveredCluster = null;
                        }
                        foundCluster = false;

                        if (hoveredCluster == null) {
                            //render initial selection box
                            if (adi.firstPos == null) {
                                AABBOutline initialSelectionPos = new AABBOutline(new AABB(VectorConversionsMCKt.toBlockPos(hoveredBlockPos)));
                                initialSelectionPos.getParams().colored(HOVERPURPLE).lightmap(9).withFaceTexture(AllSpecialTextures.SELECTION);
                                initialSelectionPos.render(poseStack, SuperRenderTypeBuffer.getInstance(), Vec3.ZERO, tickDelta);
                            }
                        } else {
                            //edit hovered cluster color
                            clusterOutlines.get(hoveredCluster).getParams().colored(HOVERPURPLE);
                        }
                        if (adi.firstPos != null) {
                            //render selection box
                            selectionBox.getParams().colored(HOVERPURPLE).lightmap(9).withFaceTexture(AllSpecialTextures.SELECTION);
                            selectionBox.target(new AABB(VectorConversionsMCKt.toBlockPos(adi.firstPos), VectorConversionsMCKt.toBlockPos(hoveredBlockPos)));
                        } else {
                            selectionBox = new ChasingAABBOutline(new AABB(0,0,0,0,0,0));
                        }
                    }
                    for (BlockClusterOutline clusterOutline : clusterOutlines.values()) {
                        clusterOutline.render(poseStack, SuperRenderTypeBuffer.getInstance(), Vec3.ZERO, tickDelta);
                    }
                }
            }
        }
    }
}