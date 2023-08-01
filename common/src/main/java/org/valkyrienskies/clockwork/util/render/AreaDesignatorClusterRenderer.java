package org.valkyrienskies.clockwork.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.outliner.BlockClusterOutline;
import com.simibubi.create.foundation.outliner.ChasingAABBOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.*;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.joml.primitives.Intersectionf;
import org.joml.primitives.LineSegmentf;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorItem;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AreaDesignatorClusterRenderer {

    private Object bbOutlineSlotAD = new Object();

    private HashMap<Set<AABBic>, Pair<Set<BlockPos>, String>> storedClusters = new HashMap<>();

    public static AreaDesignatorClusterRenderer INSTANCE = new AreaDesignatorClusterRenderer();

    private Set<AABBic> hoveredCluster = new HashSet<>();

    private static final Color HOVERPURPLE = new Color(203,195,227);

    private static final Color IDLEPURPLE = new Color(221,160,221);

    private static final String clusterID = "clusterID_";

    private static Integer clusterIncrement = 0;


    public void renderDesignator(final ClientLevel level, final Minecraft minecraft, final PoseStack poseStack, final float tickDelta, final long nanos, final boolean shouldRenderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix) {
        if (level != null) {
            for (Player player : level.players()) {
                if (player.getMainHandItem().is(ClockWorkItems.AURIC_DESIGNATOR.get())) {
                    //other players
                    AreaDesignatorItem adi = (AreaDesignatorItem) player.getMainHandItem().getItem();

                    Set<Set<AABBic>> clusters = adi.selectionClusters;

                    for (Set<AABBic> cluster : clusters) {
                        if (!storedClusters.containsKey(cluster)) {
                            storedClusters.put(cluster, Pair.of(adi.blocksFromCluster(cluster), clusterID + clusterIncrement));
                            clusterIncrement++;
                        }
                    }

                    while (!adi.toStopRendering.isEmpty()) {
                        ClockWorkMod.OUTLINER.remove(adi.toStopRendering.get(0));
                        storedClusters.remove(adi.toStopRendering.remove(0));
                    }

                    if (minecraft.getCameraEntity() == null) {
                        return;
                    }
                    poseStack.pushPose();
                    //local player
                    if (player.isLocalPlayer()) {
                        Minecraft mc = Minecraft.getInstance();
                        LocalPlayer localPlayer = mc.player;
                        if (localPlayer == null) {
                            return;
                        }
                        BlockPos hovered = null;
                        BlockPos hoveredFace = null;
                        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                            hovered = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                            hoveredFace = ((BlockHitResult) mc.hitResult).getBlockPos().relative(((BlockHitResult) mc.hitResult).getDirection());
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
                                int intersection = org.joml.primitives.Intersectionf.intersectLineSegmentAab(cast, new AABBi(box), new Vector2f());
                                if (intersection != Intersectionf.OUTSIDE) {
                                    hoveredCluster = cluster;
                                    foundCluster = true;
                                    break;
                                }
                            }
                            if (foundCluster) break;
                        }
                        if (!foundCluster) {
                            hoveredCluster = null;
                        }

                        if (hoveredCluster == null) {
                            //render initial selection box
                            if (adi.firstPos == null) {
                                Vec3 vec = (VectorConversionsMCKt.toMinecraft(new Vector3d(hoveredBlockPos)));
                                if (!vec.equals(localPlayer.getEyePosition())) {
                                    ClockWorkMod.OUTLINER.chaseAABB(adi, new AABB(VectorConversionsMCKt.toBlockPos(hoveredBlockPos)));
                                    ClockWorkMod.OUTLINER.edit(adi).ifPresent(outline -> outline.colored(HOVERPURPLE).withFaceTexture(AllSpecialTextures.SELECTION));
                                } else {
                                    ClockWorkMod.OUTLINER.remove(adi);
                                }
                            }
                        }
                        if (adi.firstPos != null) {
                            Vec3 vec = (VectorConversionsMCKt.toMinecraft(new Vector3d(hoveredBlockPos)));
                            if (!vec.equals(localPlayer.getEyePosition())) {
                                ClockWorkMod.OUTLINER.chaseAABB(bbOutlineSlotAD, new AABB(VectorConversionsMCKt.toBlockPos(adi.firstPos), VectorConversionsMCKt.toBlockPos(hoveredBlockPos)).expandTowards(1,1,1));
                                ClockWorkMod.OUTLINER.edit(bbOutlineSlotAD).ifPresent(outline -> outline.colored(HOVERPURPLE).withFaceTexture(AllSpecialTextures.SELECTION));
                            } else {
                                ClockWorkMod.OUTLINER.chaseAABB(bbOutlineSlotAD, new AABB(VectorConversionsMCKt.toBlockPos(adi.firstPos), VectorConversionsMCKt.toBlockPos(adi.firstPos)));
                            }
                            //render selection box
                        } else {
                            ClockWorkMod.OUTLINER.remove(bbOutlineSlotAD);
                        }
                    }
                    for (Set<AABBic> key : storedClusters.keySet()) {

                        ClockWorkMod.OUTLINER.showCluster(storedClusters.get(key).getRight(), storedClusters.get(key).getLeft());
                        ClockWorkMod.OUTLINER.edit(storedClusters.get(key).getRight()).ifPresent(outline -> outline.colored(IDLEPURPLE));
                        if (key.equals(hoveredCluster)) {
                            ClockWorkMod.OUTLINER.edit(storedClusters.get(key).getRight()).ifPresent(outline -> outline.colored(HOVERPURPLE));
                        }
                    }

//                    initialSelectionBox.tick();
//                    selectionBox.tick();
                    poseStack.popPose();
                }
            }
        }
    }
}
