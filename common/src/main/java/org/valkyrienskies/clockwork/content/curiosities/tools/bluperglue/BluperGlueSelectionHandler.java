package org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue;

import com.google.common.base.Objects;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.ClockWorkPackets;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BluperGlueSelectionHandler {

    private static final int PASSIVE = 0x4d8c91;
    private static final int HIGHLIGHT = 0x68c5c2;
    private static final int FAIL = 0x7c48c5;

    private Object clusterOutlineSlot = new Object();
    private Object bbOutlineSlot = new Object();
    private int clusterCooldown;

    private BlockPos firstPos;
    private BlockPos hoveredPos;
    private Set<BlockPos> currentCluster;
    public Set<Entity> currentEntityCluster;
    private int glueRequired;

    private BluperGlueEntity selected;
    private BlockPos soundSourceForRemoval;

    private Vec3 newTarget;

    private Vec3 getTraceOrigin(Player playerIn) {
        Minecraft mc = Minecraft.getInstance();
        double range = ReachEntityAttributes.getReachDistance(playerIn, mc.gameMode.getPickRange()) + 1;
        Vec3 origin = RaycastHelper.getTraceOrigin(playerIn);
        Vec3 target = RaycastHelper.getTraceTarget(playerIn, range, origin);


        AABB searchAABB = new AABB(origin, target).inflate(0.25, 2, 0.25);
        final Iterator<Ship> ships = VSGameUtilsKt.getShipsIntersecting(playerIn.level, searchAABB).iterator();

        if (ships.hasNext()) {
            Ship ship = ships.next();

            Matrix4d world2Ship = (Matrix4d) ship.getTransform().getWorldToShip();
            AABBic shAABBi = ship.getShipAABB();
            AABB shipAABB = new AABB(shAABBi.minX(), shAABBi.minY(), shAABBi.minZ(), shAABBi.maxX(), shAABBi.maxY(), shAABBi.maxZ());


            origin = VectorConversionsMCKt.toMinecraft(world2Ship.transformPosition(VectorConversionsMCKt.toJOML(origin)));
            target = VectorConversionsMCKt.toMinecraft(world2Ship.transformPosition(VectorConversionsMCKt.toJOML(target)));

            Quaterniond tempQuat = new Quaterniond();
            if (playerIn.getVehicle() != null && playerIn.getVehicle().getBoundingBox().intersects(shipAABB.inflate(20))) {
                ship.getTransform().getWorldToShip().getNormalizedRotation(tempQuat);
                tempQuat.invert();
                Vector3d offset = VectorConversionsMCKt.toJOML(target.subtract(origin));
                tempQuat.transform(offset);
                target = origin.add(VectorConversionsMCKt.toMinecraft(offset));
            }
        }

        newTarget = target;
        return origin;
    }

    private Vec3 getTraceTarget(final Player playerIn, final double range, final Vec3 origin) {
        return newTarget;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        BlockPos hovered = null;
        ItemStack stack = player.getMainHandItem();

        if (!isBluperGlue(stack)) {
            if (firstPos != null)
                discard();
            return;
        }

        if (clusterCooldown > 0) {
            if (clusterCooldown == 25)
                player.displayClientMessage(Components.immutableEmpty(), true);
            CreateClient.OUTLINER.keep(clusterOutlineSlot);
            clusterCooldown--;
        }

        AABB scanArea = player.getBoundingBox()
                .inflate(32, 16, 32);

        List<BluperGlueEntity> glueNearby = mc.level.getEntitiesOfClass(BluperGlueEntity.class, scanArea);

        selected = null;
        if (firstPos == null) {
            double range = ReachEntityAttributes.getReachDistance(player, mc.gameMode.getPickRange()) + 1;
            Vec3 traceOrigin = getTraceOrigin(player);
            Vec3 traceTarget = getTraceTarget(player, range, traceOrigin);

            double bestDistance = Double.MAX_VALUE;
            for (BluperGlueEntity glueEntity : glueNearby) {
                Optional<Vec3> clip = glueEntity.getBoundingBox()
                        .clip(traceOrigin, traceTarget);
                if (clip.isEmpty())
                    continue;
                Vec3 vec3 = clip.get();
                double distanceToSqr = vec3.distanceToSqr(traceOrigin);
                if (distanceToSqr > bestDistance)
                    continue;
                selected = glueEntity;
                soundSourceForRemoval = new BlockPos(vec3);
                bestDistance = distanceToSqr;
            }

            for (BluperGlueEntity glueEntity : glueNearby) {
                boolean h = clusterCooldown == 0 && glueEntity == selected;
                AllSpecialTextures faceTex = h ? AllSpecialTextures.GLUE : null;
                CreateClient.OUTLINER.showAABB(glueEntity, glueEntity.getBoundingBox())
                        .colored(h ? HIGHLIGHT : PASSIVE)
                        .withFaceTextures(faceTex, faceTex)
                        .disableNormals()
                        .lineWidth(h ? 1 / 16f : 1 / 64f);
            }
        }

        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == Type.BLOCK)
            hovered = ((BlockHitResult) hitResult).getBlockPos();

        if (hovered == null) {
            hoveredPos = null;
            return;
        }

        if (firstPos != null && !firstPos.closerThan(hovered, 24)) {
            Lang.translate("super_glue.too_far")
                    .color(FAIL)
                    .sendStatus(player);
            return;
        }

        boolean cancel = player.isSteppingCarefully();
        if (cancel && firstPos == null)
            return;

        AABB currentSelectionBox = getCurrentSelectionBox();

        boolean unchanged = Objects.equal(hovered, hoveredPos);

        if (unchanged) {
            if (currentCluster != null) {
                boolean canReach = currentCluster.contains(hovered);
                boolean canAfford = BluperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);
                int color = HIGHLIGHT;
                String key = "super_glue.click_to_confirm";

                if (!canReach) {
                    color = FAIL;
                    key = "super_glue.cannot_reach";
                } else if (!canAfford) {
                    color = FAIL;
                    key = "super_glue.not_enough";
                } else if (cancel) {
                    color = FAIL;
                    key = "super_glue.click_to_discard";
                }

                Lang.translate(key)
                        .color(color)
                        .sendStatus(player);

                if (currentSelectionBox != null)
                    CreateClient.OUTLINER.showAABB(bbOutlineSlot, currentSelectionBox)
                            .colored(canReach && canAfford && !cancel ? HIGHLIGHT : FAIL)
                            .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
                            .disableNormals()
                            .lineWidth(1 / 16f);

                CreateClient.OUTLINER.showCluster(clusterOutlineSlot, currentCluster)
                        .colored(0x4d8c91)
                        .disableNormals()
                        .lineWidth(1 / 64f);
            }

            return;
        }

        hoveredPos = hovered;

        Set<BlockPos> cluster = BluperGlueSelectionHelper.searchGlueGroup(mc.level, firstPos, hoveredPos, true);

        currentCluster = cluster;

        glueRequired = 1;
    }

    private boolean isBluperGlue(ItemStack stack) {
        return stack.getItem() instanceof BluperGlueItem;
    }

    private AABB getCurrentSelectionBox() {
        return firstPos == null || hoveredPos == null ? null : new AABB(firstPos, hoveredPos).expandTowards(1, 1, 1);
    }

    public boolean onMouseInput(boolean attack) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (!isBluperGlue(player.getMainHandItem()))
            return false;

        if (attack) {
            if (selected == null)
                return false;
            ClockWorkPackets.sendToServer(new BluperGlueRemovalPacket(selected.getId(), soundSourceForRemoval));
            selected = null;
            clusterCooldown = 0;
            return true;
        }

        if (player.isSteppingCarefully()) {
            if (firstPos != null) {
                discard();
                return true;
            }
            return false;
        }

        if (hoveredPos == null)
            return false;

        Direction face = null;
        if (mc.hitResult instanceof BlockHitResult bhr) {
            face = bhr.getDirection();
            BlockState blockState = level.getBlockState(hoveredPos);
        }

        player.swing(InteractionHand.MAIN_HAND);
        if (firstPos != null && currentCluster != null) {
            boolean canReach = currentCluster.contains(hoveredPos);
            boolean canAfford = BluperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);

            if (!canReach || !canAfford)
                return true;

            confirm();
            return true;
        }

        firstPos = hoveredPos;
        if (face != null)
            BluperGlueItem.spawnParticles(level, firstPos, face, true);
        Lang.translate("super_glue.first_pos")
                .sendStatus(player);
        AllSoundEvents.SLIME_ADDED.playAt(level, firstPos, 0.5F, 0.85F, false);
        level.playSound(player, firstPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1);
        return true;
    }

    public void discard() {
        LocalPlayer player = Minecraft.getInstance().player;
        currentCluster = null;
        firstPos = null;
        Lang.translate("super_glue.abort")
                .sendStatus(player);
        clusterCooldown = 0;
    }

    public void confirm() {
        LocalPlayer player = Minecraft.getInstance().player;
        ClockWorkPackets.sendToServer(new BluperGlueSelectionPacket(firstPos, hoveredPos));
        AllSoundEvents.SLIME_ADDED.playAt(player.level, hoveredPos, 0.5F, 0.95F, false);
        player.level.playSound(player, hoveredPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1);

        if (currentCluster != null)
            CreateClient.OUTLINER.showCluster(clusterOutlineSlot, currentCluster)
                    .colored(0xD1FFF5 )
                    .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .disableNormals()
                    .lineWidth(1 / 24f);

        discard();
        Lang.translate("super_glue.success")
                .sendStatus(player);
        clusterCooldown = 40;
    }

}
