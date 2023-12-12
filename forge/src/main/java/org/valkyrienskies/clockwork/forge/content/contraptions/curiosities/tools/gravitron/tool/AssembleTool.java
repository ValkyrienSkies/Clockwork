package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;


import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3ic;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronGrabPacket;
import org.valkyrienskies.clockwork.util.ClockworkUtils;
import org.valkyrienskies.core.api.util.functions.IntTernaryConsumer;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.valkyrienskies.mod.common.assembly.ShipAssemblyKt.createNewShipWithBlocks;

public class AssembleTool extends GravitronToolBase {

    @Override
    public boolean handleRightClick() {
        updateTargetPos();
        ClockworkPackets.sendToServer(new GravitronGrabPacket(clickedPos, clickedLocation, ASSEMBLE));
        return true;
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }

    public static boolean assemble(Level level, Player player, BlockPos blockPos, Vec3 clickLocation) {
        AreaData data = AreaData.of(player).get();
        HashSet<Set<AABBic>> list = data.getArea().getSelectionClusters();
        boolean bl = false;
        System.out.println("List");
        for (Set<AABBic> cluster : list) {
            System.out.println("Dense");
            DenseBlockPosSet selection = SelectedAreaToolkit.Companion.denseBlocksFromCluster(cluster);

            if (selection.isEmpty() || !selection.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
                continue;
            }
            System.out.println("Selection: " + selection.stream().findFirst());
            System.out.println("Selection not empty?" + selection.isEmpty() + " : " + selection.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

            Optional<Vector3ic> optional = selection.stream().findFirst();
            if (optional.isPresent()) {
                var vector3ic = optional.get();
                int x =  vector3ic.x();
                int y =  vector3ic.y();
                int z =  vector3ic.z();

                System.out.println("[" + x + " " + y + " " + z + "]");

                if (level instanceof ServerLevel serverLevel) {

                    if (!serverLevel.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
                        var connectedShip = createNewShipWithBlocks(blockPos, selection, serverLevel);
                        System.out.println("Ship Created");
                        Set<Entity> caughtEntities = SelectedAreaToolkit.Companion.entitiesFromCluster(cluster, serverLevel);
                        if (!caughtEntities.isEmpty()) {
                            caughtEntities.forEach(entity -> {
                                if (entity instanceof AbstractContraptionEntity || entity instanceof SuperGlueEntity || entity instanceof SeatEntity) {
                                    if (entity instanceof SuperGlueEntity) {
                                        AABB oldBounds = entity.getBoundingBox();
                                        Vector3d oldMax = new Vector3d(oldBounds.maxX, oldBounds.maxY, oldBounds.maxZ);
                                        Vector3d oldMin = new Vector3d(oldBounds.minX, oldBounds.minY, oldBounds.minZ);
                                        Vector3d newMax = connectedShip.getTransform().getWorldToShip().transformPosition(oldMax, new Vector3d());
                                        Vector3d newMin = connectedShip.getTransform().getWorldToShip().transformPosition(oldMin, new Vector3d());
                                        AABB newBounds = new AABB(newMin.x, newMin.y, newMin.z, newMax.x, newMax.y, newMax.z);
                                        entity.setBoundingBox(newBounds);
                                        ((SuperGlueEntity) entity).resetPositionToBB();
                                    } else {
                                        Vector3d oldPos = VectorConversionsMCKt.toJOML(entity.position());
                                        Vector3d newPos = connectedShip.getTransform().getWorldToShip().transformPosition(oldPos, new Vector3d());
                                        entity.moveTo(VectorConversionsMCKt.toMinecraft(newPos));
                                    }
                                }
                            });
                        }

                        Vec3 grabPosInShip = clickLocation;
                        CompoundTag tag = player.getMainHandItem().getOrCreateTag();
                        tag.putLong("ShipId", connectedShip.getId());
                        tag.put("GrabbedPosInShip", ClockworkUtils.writeVec3(grabPosInShip));

                        return true;
                    }
                }
            } else {
                return false;
            }

            data.getArea().getToStopRendering().add(cluster);
            data.shouldReset(true);
        }
        return false;
    }
}