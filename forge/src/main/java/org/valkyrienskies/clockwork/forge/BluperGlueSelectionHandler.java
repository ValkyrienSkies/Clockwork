package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler;
import com.simibubi.create.content.schematics.client.SchematicAndQuillHandler;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.utility.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

public class BluperGlueSelectionHandler {

    private Object outlineSlot = new Object();

    private BlockPos selectedPos;
    private Direction selectedFace;
    private HashMap<Set<AABBic>, Pair<Set<BlockPos>, String>> storedClusters = new HashMap<>();
    private int range = 10;

    private String clusterID = "clusterID_";
    private int clusterIncrement = 0;
    private Color IDLEPURPLE = new Color(221, 160, 221);

    public void discard() {
        LocalPlayer player = Minecraft.getInstance().player;
        var data = AreaData.of(player).get();
        data.setFirstPos(Optional.empty());
        data.setSecondPos(Optional.empty());
        storedClusters = new HashMap<>();
        Lang.translate("schematicAndQuill.abort")
                .sendStatus(player);
    }

    public void tick() {
        if (!isActive())
            return;

        LocalPlayer player = Minecraft.getInstance().player;
        var data = AreaData.of(player).get();
        if (AllKeys.ACTIVATE_TOOL.isPressed()) {
            float pt = AnimationTickHolder.getPartialTicks();
            Vec3 targetVec = player.getEyePosition(pt).add(player.getLookAngle().scale(range));
            selectedPos = BlockPos.containing(targetVec);

        } else {
            BlockHitResult trace = RaycastHelper.rayTraceRange(player.level(), player, 75);
            if (trace != null && trace.getType() == HitResult.Type.BLOCK) {
                selectedPos = trace.getBlockPos();
            } else
                selectedPos = null;
        }

        Set<Set<AABBic>> clusters = data.getArea().getSelectionClusters();

        for (Set<AABBic> cluster : clusters) {
            if (!storedClusters.containsKey(cluster)) {
                storedClusters.put(cluster, Pair.of(SelectedAreaToolkit.Companion.blocksFromCluster(cluster), clusterID + clusterIncrement++));
            }
        }

        selectedFace = null;
        if (data.getSecondPos().isPresent()) {
            AABB bb = new AABB(data.getFirstPos().get(), data.getSecondPos().get()).expandTowards(1, 1, 1).inflate(.45f);
            Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            boolean inside = bb.contains(projectedView);
            RaycastHelper.PredicateTraceResult result = RaycastHelper.rayTraceUntil(player, 70, pos -> inside ^ bb.contains(VecHelper.getCenterOf(pos)));
            selectedFace = result.missed() ? null : inside ? result.getFacing().getOpposite() : result.getFacing();
        }

        AABB currentSelectionBox = getCurrentSelectionBox(data);
        if (currentSelectionBox != null)
            outliner().chaseAABB(outlineSlot, currentSelectionBox)
                    .colored(0x6886c5)
                    .withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .lineWidth(1 / 16f)
                    .highlightFace(selectedFace);

        renderStoredClusters();
    }

    private AABB getCurrentSelectionBox(AreaData data) {
        if (data.getSecondPos().isEmpty()) {
            if (data.getFirstPos().isEmpty())
                return selectedPos == null ? null : new AABB(selectedPos);
            return selectedPos == null ? new AABB(data.getFirstPos().get()) : new AABB(data.getFirstPos().get(), selectedPos).expandTowards(1, 1, 1);
        }
        return new AABB(data.getFirstPos().get(), data.getSecondPos().get()).expandTowards(1, 1, 1);
    }

    private void renderStoredClusters() {
        for (Set<AABBic> key : storedClusters.keySet()) {
            var storedCluster = storedClusters.get(key);

            ClockworkMod.getOUTLINER().showCluster(
                    storedCluster.getSecond(), storedCluster.getFirst()
            );

            ClockworkMod.getOUTLINER().edit(storedCluster.getSecond()).ifPresent(outline ->
                    outline.colored(IDLEPURPLE)
            );
        }
    }

    private boolean isActive() {
        return isPresent() && (ClockworkItems.BLUPERGLUE.isIn(Minecraft.getInstance().player.getMainHandItem()) || ClockworkItems.GRAVITRON.isIn(Minecraft.getInstance().player.getMainHandItem()));
    }

    private boolean isPresent() {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().screen == null;
    }

    private Outliner outliner() {
        return ClockworkMod.getOUTLINER();
    }

}
