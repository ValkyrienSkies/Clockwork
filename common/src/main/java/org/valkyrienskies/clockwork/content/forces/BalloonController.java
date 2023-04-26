package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
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

import java.util.Set;
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
                    createData.right().fuelQuality()
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
            physData.fuelQuality = data.fuelQuality();
            physData.burnTemp = data.burnTemp();
            physData.rpm = data.rpm();
        });

        balloonUpdateData.clear();

        Vector3dc torque = resistRotation((PhysShipImpl) physShip);
        Vector3dc drag = airResistance((PhysShipImpl) physShip);
//        physShip.applyRotDependentTorque(torque);
        physShip.applyInvariantTorque(torque);
        physShip.applyInvariantForce(drag);

        for (BalloonData physData : balloonData.values()) {
            Set<Vector3dc> vol = physData.volume;
            if (vol.isEmpty()) {
                continue;
            }
            Vector3dc force = computeForce(physData, (PhysShipImpl) physShip, vol);
            double x = 0;
            double y = 0;
            double z = 0;

            //todo: BETTER SOLUTION
            for (Vector3dc pos : vol) {
                Vector3d tPos = pos.sub(physShip.getTransform().getPositionInShip(), new Vector3d());
                x += (tPos.x() + 0.5);
                y += (tPos.y() + 0.5);
                z += (tPos.z() + 0.5);
            }
            x /= vol.size();
            y /= vol.size();
            z /= vol.size();
            Vector3dc center = new Vector3d(x, y, z);

//            Vector3dc centervec = center.sub(physShip.getTransform().getPositionInShip(), new Vector3d());
            if(force.y() > 0) {
//                System.out.println("Balloon " + force + " , " + center);
                physShip.applyInvariantForceToPos(force, center);
            }
        }
    }

    private Vector3dc computeForce(BalloonData physData, PhysShipImpl physShip, Set<Vector3dc> vol) {
        double volume = vol.size();

        if (volume == 0) {
            return new Vector3d();
        }

        double earthAirPressure = 101325; // Pascals
        double roomAirTemp = 293; // Kelvin, Equiv 20C
        double gasConstant = 287.05; //gas constant for air j/(kg*degK)
        double maxTemp = 380; // to achieve max temp of 400c / 673k
        double gravity = 9.8; // m/s^2


        //earth air pressure in Pa * air pressure gradient modifier
        double airPress = earthAirPressure * airPressure(physShip.getTransform().getPositionInWorld());
        // converting it to kelvin (20 degrees celsius with a max temp of 400c modified by heat)
        double internalTemp = roomAirTemp + (maxTemp * physData.burnTemp);
        // density of air at temperature in balloon
        double internalDensity = airPress/(internalTemp * gasConstant);
        // density of air at 20c
        double standardDensity = airPress/(roomAirTemp * gasConstant);
        // f = v * dp * g
        double force = volume * (standardDensity - internalDensity) * gravity;

        Vector3dc forceVec = new Vector3d(0, force*50000, 0);
        return forceVec;
    }

    private Vector3d resistRotation(PhysShipImpl physShip) {
        double resistance = 0.05;

        resistance = resistance;
//
        Vector3d torque = physShip.getPoseVel().getOmega().mul(physShip.getInertia().getMomentOfInertiaTensor(), new Vector3d()).mul(-resistance);
//        Vector3d torque = physShip.getPoseVel().getOmega().mul(-resistance, new Vector3d());
        return torque;
    }

    private Vector3d airResistance(PhysShipImpl physShip) {
        double resistance = 0.01;

        return physShip.getPoseVel().getVel().mul(physShip.getInertia().getShipMass(), new Vector3d()).mul(-resistance);
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
