package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;


import com.simibubi.create.AllPackets;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.content.schematics.packet.SchematicPlacePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkPackets;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducer;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.GravitronGrabPacket;
import org.valkyrienskies.clockwork.util.ClockworkUtils;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import static java.lang.Math.toRadians;

public class GrabTool extends GravitronToolBase {

    public static void tick(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Player player && player.level() instanceof ServerLevel serverLevel) {
            var s = getState(player);
            var graviton = player.getMainHandItem();
            //System.out.println(s.getShipID() + " : " + s.getGrabbing());
            if (true) {
                if (!s.getShouldDrop() && graviton.is(ClockworkItems.GRAVITRON.asItem())) {
                    updateShip(s, serverLevel, entity);
                } else {
                    dropShip(s, serverLevel);
                }
                //MAIN LEVEL

                if (s.getGrabCD() != null && s.getGrabCD() > 0) {
                    s.setGrabCD(s.getGrabCD() - 1);
                }
                if (graviton.hasTag() && graviton.getTag().contains("GrabbedPosInShip") && !player.getCooldowns().isOnCooldown(graviton.getItem())) {
                    s.setGrabbing(true);
                    var tag = graviton.getTag();

                    var clickLocation = ClockworkUtils.readVec3(tag.getList("GrabbedPosInShip", Tag.TAG_DOUBLE));
                    var id = tag.getLong("ShipId");

                    var ship = VSGameUtilsKt.getShipObjectWorld(serverLevel).getLoadedShips().getById(id);
                    if (ship != null) {
                        System.out.println(ship);
                        var transformedPos = ship.getWorldToShip().transformPosition(VectorConversionsMCKt.toJOML(clickLocation), new Vector3d());
                        grabShip(s, player, ship, transformedPos);
                        graviton.removeTagKey("ShipId");
                        graviton.removeTagKey("GrabbedPosInShip");
                    }
                }
            } else {

            }
        }

    }

    private static void dropShip(GravitronItem.Companion.GravitronState s, ServerLevel level) {
        var grabbedShipId = s.getShipID();
        if (grabbedShipId != null) {
            var loadedShip = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getById(grabbedShipId);
            if (loadedShip != null) {
                var gravitronForceInducer = GravitronForceInducer.Companion.getOrCreate(loadedShip);
                gravitronForceInducer.setData(null);
            }
        }

        s.setGrabbing(false);
        s.setShipID(null);
        s.setShouldDrop(false);
    }

    private static void updateShip(GravitronItem.Companion.GravitronState s , ServerLevel level, Entity entity) {
        if (s.getGrabbing()) {
            var shipId = s.getShipID();
            if (shipId != null) {
                var shipUnloaded = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(shipId);
                var ship = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getById(shipId);
                if (ship != null && s.getPlayerGrabbedRotation() != null && s.getShipGrabbedDistance() != null && s.getShipGrabbedPos() != null && s.getHeldBlockPos() != null) {
                    // Update Rot Values
                    var playerCurrentRotation = new Vector2d(entity.getXRot(), entity.getYRot());
                    var origPlayerRot = playerRotToQuaternion(s.getPlayerGrabbedRotation().x(), s.getPlayerGrabbedRotation().y()).normalize();
                    var newPlayerRot = playerRotToQuaternion(playerCurrentRotation.x(), playerCurrentRotation.y()).normalize();
                    var deltaPlayerRot = newPlayerRot.mul(origPlayerRot.conjugate(new Quaterniond()), new Quaterniond());
                    var rotation = deltaPlayerRot.mul(s.getShipGrabbedRot(), new Quaterniond()).normalize();

                    // Update Pos Values
                    var lookDif = VectorConversionsMCKt.toJOML(entity.getLookAngle()).normalize().mul(s.getShipGrabbedDistance());
                    s.setHeldBlockPos(VectorConversionsMCKt.toJOML(entity.getEyePosition()).add(lookDif));
                    var location = new Vector3d(s.getShipGrabbedPos());
                    var position = new Vector3d(s.getHeldBlockPos());

                    var gravitronForceInducer = GravitronForceInducer.Companion.getOrCreate(ship);
                    var newData = new GravitronForceInducerData(position, rotation, location);
                    gravitronForceInducer.setData(newData);
                } else if (shipUnloaded == null) {
                    dropShip(s, level);
                }
            }
        }
    }

    private static Quaterniond playerRotToQuaternion(double pitch, double yaw) {
        return new Quaterniond().rotateY(toRadians(-yaw)).rotateX(toRadians(pitch));
    }

    @Override
    public boolean handleRightClick() {
        updateTargetPos();

        ClockworkPackets.sendToServer(new GravitronGrabPacket(clickedPos, clickedLocation));

        return false;
    }

    @Override
    public boolean handleMouseWheel(double delta) {
        return false;
    }

    public static boolean tryGrabShip(ServerLevel level, Player player, BlockPos clickedPos, Vec3 clickLocation) {
        int chunkX = clickedPos.getX() >> 4;
        int chunkZ = clickedPos.getZ() >> 4;
        System.out.println("PreGrab0: " + clickedPos + " : " + clickLocation);
        LoadedServerShip ship = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getByChunkPos(chunkX, chunkZ, VSGameUtilsKt.getDimensionId(level));
        Vector3dc grabPosInShip = VectorConversionsMCKt.toJOML(clickLocation);
        Vector3d grabPosInWorld = new Vector3d(grabPosInShip);
        var s = getState(player);
        System.out.println("PreGrab: " + grabPosInShip + " : " + grabPosInWorld);
        if (VSGameUtilsKt.isBlockInShipyard(level, clickedPos) && ship == null) {
            return false;
        }

        if (ship == null) {
            return false;
        } else {
            ship.getShipToWorld().transformPosition(grabPosInWorld);
        }
        System.out.println("Grab");
        grabShip(s, player, ship, grabPosInShip);
        return true;
    }

    private static void grabShip(GravitronItem.Companion.GravitronState s, Player p, LoadedServerShip ship, Vector3dc grabPosInShip) {
        Vector3d heldPosInWorld = new Vector3d();
        ship.getTransform().getShipToWorld().transformPosition(new Vector3d(grabPosInShip), heldPosInWorld);

        s.setShipID(ship.getId());
        s.setHeldBlockPos(heldPosInWorld);
        s.setPlayerGrabbedRotation(new Vector2d(p.getXRot(), p.getYRot()));
        s.setShipGrabbedPos(new Vector3d(grabPosInShip));
        s.setShipGrabbedRot(ship.getTransform().getShipToWorldRotation());
        s.setShipGrabbedDistance(VectorConversionsMCKt.toJOML(p.getEyePosition()).distance(heldPosInWorld));
        ship.setStatic(false);
    }
}