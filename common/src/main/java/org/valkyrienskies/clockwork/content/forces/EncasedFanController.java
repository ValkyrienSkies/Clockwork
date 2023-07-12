package org.valkyrienskies.clockwork.content.forces;

import org.valkyrienskies.clockwork.content.propulsion.fan.EncasedFanCreateData;
import org.valkyrienskies.clockwork.content.propulsion.fan.EncasedFanData;
import org.valkyrienskies.clockwork.content.propulsion.fan.EncasedFanUpdateData;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EncasedFanController implements ShipForcesInducer  {

    final Int2ObjectOpenHashMap<EncasedFanData> fanData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, EncasedFanUpdateData> fanUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, EncasedFanCreateData>> createdFans = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedFans = new ConcurrentLinkedQueue<>();

    private int nextFanID = 0;

    public static EncasedFanController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(EncasedFanController.class) == null) {
            ship.saveAttachment(EncasedFanController.class, new EncasedFanController());
        }
        return ship.getAttachment(EncasedFanController.class);
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdFans.isEmpty()) {
            final Pair<Integer, EncasedFanCreateData> createData = createdFans.remove();
            fanData.put(createData.left(), new EncasedFanData(
                    createData.right().fanPos(),
                    createData.right().fanDir(),
                    createData.right().fanSpeed()
            ));
        }
        while (!removedFans.isEmpty()) {
            fanData.remove((int) removedFans.remove());
        }

        fanUpdateData.forEach((id, data) -> {
            EncasedFanData physData = fanData.get(id);
            if (physData == null) {
                return;
            }
            physData.fanSpeed = data.fanSpeed();
        });

        fanUpdateData.clear();

        for (EncasedFanData physData : fanData.values()) {
            Vector3dc force = computeForce(physData, ((PhysShipImpl) physShip));
            Vector3dc fanVector = physData.fanPos.add(0.5,0.5,0.5, new Vector3d()).sub(physShip.getTransform().getPositionInShip());
            physShip.applyRotDependentForceToPos(force, fanVector);
        }
    }

    private Vector3dc computeForce(EncasedFanData physData, PhysShipImpl physShip) {
        double speed = physData.fanSpeed;

        Vector3d dir = new Vector3d(physData.fanDir).mul(Math.signum(speed));

        double providedForce = (Math.abs(speed) * 36.00875);
        double airPress = airPressure(physShip.getTransform().getPositionInWorld());
        Vector3dc fanPosRelCenterMass = physShip.getTransform().getShipToWorld().transformPosition(physData.fanPos.add(0.5,0.5,0.5, new Vector3d()), new Vector3d()).sub(physShip.getTransform().getPositionInWorld(), new Vector3d());
        Vector3dc worldVelAtFan = physShip.getPoseVel().getOmega().cross(fanPosRelCenterMass, new Vector3d()).add(physShip.getPoseVel().getVel(), new Vector3d());
        double exhaustVel = exhaustVelocity();


        double factor = 1.0 - Mth.clamp(dir.dot(worldVelAtFan) / exhaustVel, 0.0, 1.0);
        if (!Double.isFinite(factor)) {
            factor = 0;
        }

        providedForce = providedForce * airPress * factor;


        Vector3dc force = dir.mul(providedForce);

        return force;
    }

    private double airPressure(Vector3dc pos) {
        final double SEA_LEVEL = 64.0;
        final double WORLD_HEIGHT = 320.0;
        final double FALLOFF_POINT = 192.0;

        double offset = Math.exp(-(WORLD_HEIGHT-SEA_LEVEL)/FALLOFF_POINT);
        double height = pos.y();
        double airPress = (Math.exp(-(height-SEA_LEVEL)/FALLOFF_POINT)-offset)/(1.0-offset);
        if (Double.isFinite(airPress)) {
            return Mth.clamp(airPress, 0, 1);
        } else {
            return 0.0;
        }
    }

    private double exhaustVelocity() {
        return 44.074;
    }

    public int addEncasedFan(EncasedFanCreateData data) {
        int id = nextFanID++;
        createdFans.add(Pair.of(id, data));
        return id;
    }

    public void removeEncasedFan(int id) {
        removedFans.add(id);
    }

    public void updateEncasedFan(int id, EncasedFanUpdateData data) {
        fanUpdateData.put(id, data);
    }

}