package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorCreatePhysData;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorPhysData;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorUpdatePhysData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.lang.Math;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PropellorController implements ShipForcesInducer {

    final Int2ObjectOpenHashMap<PropellorPhysData> propellorPhysData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, PropellorUpdatePhysData> propellorUpdatePhysData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, PropellorCreatePhysData>> createdProps = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedProps = new ConcurrentLinkedQueue<>();

    private int nextPropID = 0;

    public Pair<Vector3d, Vector3d> lastForceNTorque;

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdProps.isEmpty()) {
            final Pair<Integer, PropellorCreatePhysData> createData = createdProps.remove();
            propellorPhysData.put(createData.left(), new PropellorPhysData(
                    createData.right().bearingPos(),
                    createData.right().bearingAxis(),
                    createData.right().bearingAngle(),
                    createData.right().bearingSpeed(),
                    createData.right().propellorPositions()
            ));
        }
        while (!removedProps.isEmpty()) {
            propellorPhysData.remove((int) removedProps.remove());
        }

        propellorUpdatePhysData.forEach((id, data) -> {
            PropellorPhysData physData = propellorPhysData.get(id);
            if (physData == null) {
                return;
            }
            physData.bearingAngle = data.rotationAngle();
            physData.bearingSpeed = data.rotationSpeed();
        });

        propellorUpdatePhysData.clear();

        // Propellor Thrust
        Vector3d netForce = new Vector3d();
        Vector3d netTorque = new Vector3d();

        for (PropellorPhysData physData: propellorPhysData.values()) {
            Pair<Vector3dc, Vector3dc> forceTorque = computeForce(physShip.getTransform(), physData, ((PhysShipImpl) physShip).getPoseVel().getVel(), ((PhysShipImpl) physShip).getPoseVel().getOmega());
            netForce.add(forceTorque.left());
            netTorque.add(forceTorque.right());
            lastForceNTorque = Pair.of(netForce, netTorque);
        }

        if (netForce.isFinite() && netTorque.isFinite()) {
            physShip.applyInvariantForce(netForce);
            physShip.applyInvariantTorque(netTorque);
        }



        // Propellor Pushing


    }

    private Pair<Vector3dc, Vector3dc> computeForce(ShipTransform physTransform, PropellorPhysData physProp, Vector3dc vel, Vector3dc omega) {
        Vector3dc bearingVector = new Vector3d(physProp.bearingPos).add(0.5, 0.5, 0.5);
        Vector3dc axis = physProp.bearingAxis.mul(Math.signum(physProp.bearingSpeed), new Vector3d());
        Quaterniondc rotation = new Quaterniond(new AxisAngle4d(Math.toRadians(physProp.bearingAngle), axis));
        Vector3dc angVel = axis.mul((physProp.bearingSpeed/60.0) * (2.0 * Math.PI), new Vector3d());

        Vector3d netForce = new Vector3d();
        Vector3d netTorque = new Vector3d();

        for (Vector3ic pos : physProp.propellorPositions) {
            Vector3dc sailVector = (new Vector3d(pos.x(), pos.y(), pos.z())).add(bearingVector);
            Vector3dc diff = sailVector.sub(bearingVector, new Vector3d());
            Vector3dc rotatedDiff = rotation.transform(diff, new Vector3d());
            Vector3dc sailVel = rotatedDiff.cross(angVel, new Vector3d());

            Vector3d force = physTransform.getShipToWorldRotation().transform(axis.mul(sailVel.length() * 1000, new Vector3d()));
//            Vector3d force2 = force.mul(physProp.bearingSpeed, new Vector3d());
            Vector3dc sailPosWorld = physTransform.getShipToWorld().transformPosition(sailVector, new Vector3d());
            Vector3dc sailPosRelShip = sailPosWorld.sub(physTransform.getPositionInWorld(), new Vector3d());
            Vector3d torque = sailPosRelShip.cross(force, new Vector3d());
            Vector3dc sailPosRelCenterMass = physTransform.getShipToWorld().transformPosition(sailVector, new Vector3d()).sub(physTransform.getPositionInWorld(), new Vector3d());
            Vector3dc worldVelAtSail = omega.cross(sailPosRelCenterMass, new Vector3d()).add(vel, new Vector3d());
            double exhaustVel = exhaustVelocity(rotatedDiff, angVel);
            double factor = 1.0 - Mth.clamp(axis.dot(worldVelAtSail) / exhaustVel, 0.0, 1.0);
            if (!Double.isFinite(factor)) {
                factor = 1;
            }
            double airPress = airPressure(sailPosWorld);
            force.mul(airPress * factor);
            torque.mul(airPress * factor);
            netForce.add(force);
            netTorque.add(torque);
        }
        return Pair.of(netForce, netTorque);
    }

    private double airPressure(Vector3dc pos) {
        double offset = Math.exp(-(320.0-64.0)/192.0);
        double height = pos.y();
        double airPress = (Math.exp(-(height-64.0)/192)-offset)/(1.0-offset);
        if (Double.isFinite(airPress)) {
            return Mth.clamp(airPress, 0, 1);
        } else {
            return 0.0;
        }
    }

    public Pair<Vector3d, Vector3d> getLastForceNTorque() {
        return lastForceNTorque;
    }

    private double exhaustVelocity(Vector3dc posRelBearing, Vector3dc omega) {
        double vel = posRelBearing.cross(omega, new Vector3d()).length();
        return vel;
    }

    public int addPropellor(PropellorCreatePhysData data) {
        int id = nextPropID++;
        createdProps.add(Pair.of(id, data));
        return id;
    }

    public void removePropellor(int id) {
        removedProps.add(id);
    }

    public void updatePropellor(int id, PropellorUpdatePhysData data) {
        propellorUpdatePhysData.put(id, data);
    }

    public static PropellorController getOrCreate(ServerShip ship) {
        if(ship.getAttachment(PropellorController.class) == null) {
            ship.saveAttachment(PropellorController.class, new PropellorController());
        }
        return ship.getAttachment(PropellorController.class);
    }
}