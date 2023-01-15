package org.valkyrienskies.clockwork.fabric.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorCreatePhysData;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorPhysData;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorUpdatePhysData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;

import java.lang.Math;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PropellorController implements ShipForcesInducer {

    final Int2ObjectOpenHashMap<PropellorPhysData> propellorPhysData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, PropellorUpdatePhysData> propellorUpdatePhysData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, PropellorCreatePhysData>> createdProps = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedProps = new ConcurrentLinkedQueue<>();

    private int nextPropID = 0;

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
            Pair<Vector3dc, Vector3dc> forceTorque = computeForce(physShip.getTransform(), physData);
            netForce.add(forceTorque.left());
            netTorque.add(forceTorque.right());
        }

        physShip.applyInvariantForce(netForce);
        physShip.applyInvariantTorque(netTorque);
        // Propellor Pushing



    }

    public Pair<Vector3dc, Vector3dc> computeForce(ShipTransform physTransform, PropellorPhysData physProp) {
        Vector3dc bearingVector = new Vector3d(physProp.bearingPos).add(0.5, 0.5, 0.5);
        Vector3dc axis = physProp.bearingAxis;
        Quaterniondc rotation = new Quaterniond(new AxisAngle4d(Math.toRadians(physProp.bearingAngle), axis));
        Vector3dc angVel = axis.mul((physProp.bearingSpeed/60.0) * (2.0 * Math.PI), new Vector3d());

        Vector3d netForce = new Vector3d();
        Vector3d netTorque = new Vector3d();

        for (Vector3ic pos : physProp.propellorPositions) {
            Vector3dc sailVector = new Vector3d(pos.x()+0.5, pos.y()+0.5, pos.z()+0.5);
            Vector3dc diff = sailVector.sub(bearingVector, new Vector3d());
            Vector3dc rotatedDiff = rotation.transform(diff, new Vector3d());
            Vector3dc sailVel = rotatedDiff.cross(angVel, new Vector3d());

            Vector3dc force = physTransform.getShipToWorldRotation().transform(axis.mul(sailVel.lengthSquared() * 10, new Vector3d()));
            Vector3dc sailPosWorld = physTransform.getShipToWorld().transformPosition(sailVector, new Vector3d());
            Vector3dc sailPosRelShip = sailPosWorld.sub(physTransform.getPositionInWorld(), new Vector3d());
            Vector3dc torque = sailPosRelShip.cross(force, new Vector3d());

            double airPress = airPressure(sailPosWorld);
            netForce.add(force).mul(airPress);
            netTorque.add(torque).mul(airPress);
        }
        return Pair.of(netForce, netTorque);
    }

    public static double airPressure(Vector3dc pos) {
        double offset = Math.exp(-(320.0-64.0)/192.0);
        double height = pos.y();
        return (Math.exp(-(height-64.0)/192)-offset)/(1.0-offset);
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
