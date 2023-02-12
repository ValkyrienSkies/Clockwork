package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonCreateData;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonData;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BalloonController implements ShipForcesInducer {

    public final Int2ObjectOpenHashMap<BalloonData> balloonData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, BalloonUpdateData> balloonUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, BalloonCreateData>> createdBalloons = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedBalloons = new ConcurrentLinkedQueue<>();

    private int nextBalloonID = 0;

    public static BalloonController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(BalloonController.class) == null) {
            ship.saveAttachment(BalloonController.class, new BalloonController());
        }
        return ship.getAttachment(BalloonController.class);
    }


    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdBalloons.isEmpty()) {
            final Pair<Integer, BalloonCreateData> createData = createdBalloons.remove();
            balloonData.put(createData.left(), new BalloonData(
                    createData.right().burnerPos(),
                    createData.right().volume(),
                    createData.right().rpm(),
                    createData.right().burnTemp(),
                    createData.right().heatLevel()
            ));
        }
        while (!removedBalloons.isEmpty()) {
            balloonData.remove((int) removedBalloons.remove());
        }

        balloonUpdateData.forEach((id, data) -> {
            BalloonData physData = balloonData.get(id);
            if (physData == null) {
                return;
            }
            physData.volume = data.volume();
            physData.heatLevel = data.heatLevel();
            physData.burnTemp = data.burnTemp();
            physData.rpm = data.rpm();
        });

        balloonUpdateData.clear();

        for (BalloonData physData : balloonData.values()) {
            if (physData.volume.isEmpty()) {
                continue;
            }
            Vector3dc force = computeForce(physData, (PhysShipImpl) physShip);
            double x = 0;
            double y = 0;
            double z = 0;
            for (Vector3dc pos : physData.volume) {
                x += pos.x();
                y += pos.y();
                z += pos.z();
            }
            x /= physData.volume.size();
            y /= physData.volume.size();
            z /= physData.volume.size();
            Vector3dc center = new Vector3d(x, y, z);

            Vector3dc centervec = physShip.getTransform().getShipToWorld().transformPosition(center.add(0.5,0.5,0.5, new Vector3d()), new Vector3d()).sub(physShip.getTransform().getPositionInWorld(), new Vector3d());
            physShip.applyInvariantForceToPos(force, centervec);
        }
    }

    private Vector3dc computeForce(BalloonData physData, PhysShipImpl physShip) {
        double volume = physData.volume.size();
        double airPress = 101325 * airPressure(physShip.getTransform().getPositionInWorld());
        double internalTemp = 273 + (20 + (80 * physData.burnTemp));

        double internalDensity = airPress/(internalTemp * 287.05);

        double standardDensity = airPress/(293 * 287.05);
        double force = volume * (standardDensity - internalDensity) * 9.81;

        Vector3dc forceVec = new Vector3d(0, force * 1000, 0);
        return forceVec;
    }

    private double airPressure(Vector3dc pos) {
        double offset = Math.exp(-(320.0-64.0)/192.0);
        double height = pos.y();
        double airPress = (Math.exp(-(height-64.0)/255)-offset)/(1.0-offset);
        if (Double.isFinite(airPress)) {
            return Mth.clamp(airPress, 0, 1);
        } else {
            return 0.0;
        }
    }

    public int addBalloon(BalloonCreateData data) {
        int id = nextBalloonID++;
        createdBalloons.add(Pair.of(id, data));
        return id;
    }

    public void removeBalloon(int id) {
        removedBalloons.add(id);
    }

    public void updateBalloon(int id, BalloonUpdateData data) {
        balloonUpdateData.put(id, data);
    }


}
