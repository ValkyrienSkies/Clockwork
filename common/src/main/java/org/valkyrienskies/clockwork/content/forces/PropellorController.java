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
import org.valkyrienskies.core.api.ships.properties.ShipInertiaData;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl;

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
            ShipInertiaDataImpl propInertiaData = ShipInertiaDataImpl.Companion.newEmptyShipInertiaData();
            for (Vector3ic i : createData.right().propellorPositions()) {
                propInertiaData.onSetBlock(i.x(), i.y(), i.z(), 0, 100);
            }
            propellorPhysData.put(createData.left(), new PropellorPhysData(
                    createData.right().bearingPos(),
                    createData.right().bearingAxis(),
                    createData.right().bearingAngle(),
                    createData.right().bearingSpeed(),
                    createData.right().propellorPositions(),
                    createData.right().inverted(),
                    propInertiaData
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
            physData.inverted = data.inverted();
        });

        propellorUpdatePhysData.clear();

        // Propellor Thrust
        Vector3d netForce = new Vector3d();
        Vector3d netTorque = new Vector3d();

        for (PropellorPhysData physData: propellorPhysData.values()) {
            Pair<Vector3dc, Vector3dc> forceTorque = computeForce(physShip.getTransform(), physData, ((PhysShipImpl) physShip).getPoseVel().getVel(), ((PhysShipImpl) physShip).getPoseVel().getOmega(), ((PhysShipImpl) physShip));
            netForce.add(forceTorque.left());
            netTorque.add(forceTorque.right());
        }

        if (netForce.isFinite() && netTorque.isFinite()) {
            physShip.applyInvariantForce(netForce);
            physShip.applyInvariantTorque(netTorque);
        }



        // Propellor Pushing


    }

    private Pair<Vector3dc, Vector3dc> computeForce(ShipTransform physTransform, PropellorPhysData physProp, Vector3dc vel, Vector3dc omega, PhysShipImpl physShip) {
        final double modifiedSpeed = physProp.bearingSpeed;
        Vector3dc bearingVector = new Vector3d(physProp.bearingPos).add(0.5, 0.5, 0.5);
        Vector3dc axis = physProp.bearingAxis.mul(Math.signum(modifiedSpeed), new Vector3d());
        Quaterniondc rotation = new Quaterniond(new AxisAngle4d(Math.toRadians(physProp.bearingAngle), axis));
        Vector3dc angVel = axis.mul((modifiedSpeed/60.0) * (2.0 * Math.PI), new Vector3d());

        Vector3d furthestTip = new Vector3d();

        Vector3d netForce = new Vector3d();
        Vector3d netTorque = new Vector3d();

        for (Vector3ic pos : physProp.propellorPositions) {
            Vector3dc sailVector = (new Vector3d(pos.x(), pos.y(), pos.z())).add(bearingVector);
            Vector3dc diff = sailVector.sub(bearingVector, new Vector3d());
            Vector3dc rotatedDiff = rotation.transform(diff, new Vector3d());
            Vector3dc sailVel = rotatedDiff.cross(angVel, new Vector3d());
            if (rotatedDiff.length() > furthestTip.length()) {
                furthestTip.set(rotatedDiff);
            }
            Vector3d force = physTransform.getShipToWorldRotation().transform(axis.mul(sailVel.length(), new Vector3d())).mul(5000, new Vector3d());
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

//        netTorque.add(conserveMomentum(physShip, physProp, furthestTip, angVel));
        if (physProp.inverted) {
            netForce.mul(-1);
        }
//        System.out.println(netTorque);
        return Pair.of(netForce, netTorque);
    }

    private Vector3dc conserveMomentum (PhysShipImpl physShip, PropellorPhysData physProp, Vector3dc furthestTip, Vector3dc angVel) {
        Vector3dc prevAngMomentumRelProp = new Vector3d();
        if (physProp.getPrevAngularMomentum() != null) {
            prevAngMomentumRelProp = physProp.getPrevAngularMomentum();
        }
        Vector3dc propPos = physProp.bearingPos;

        Vector3dc propAxis = new Vector3d(physProp.bearingAxis);
        double propSpeed = physProp.bearingSpeed;

        // 1/2 * Mass * (Outer Wheel Radius^2 + Total Wheel Radius^2)

        //negative to fix dir
        double rotVel = (propSpeed * ((2 * Math.PI) / 60)) * -1;

        Vector3dc angularVelocityPropellor = new Vector3d(propAxis).mul(rotVel);

        Vector3dc angularMomentumRelProp = angularVelocityPropellor.mul(physProp.inertiaData.getMomentOfInertiaTensor(), new Vector3d());

        // Add to convert from momentum relative to wheel into relative to ship
        Vector3dc r = new Vector3d(physProp.inertiaData.getCenterOfMassInShip().add(physProp.bearingPos, new Vector3d())).sub(physShip.getTransform().getPositionInShip()).rotate(physShip.getTransform().getShipToWorldRotation());
        Vector3dc momentumModifier = new Vector3d(physShip.getPoseVel().getOmega()).cross(r).mul(physProp.inertiaData.getMass());

        Vector3dc angularMomentumRelShip = new Vector3d(angularMomentumRelProp).add(momentumModifier);
        Vector3dc prevAngularMomentumRelShip = new Vector3d(prevAngMomentumRelProp).add(momentumModifier);

        Vector3dc torque = new Vector3d(prevAngularMomentumRelShip).sub(angularMomentumRelShip).div(1 / 60.0);

        physProp.setPrevAngularMomentum(angularMomentumRelProp);

        return torque;
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

    private double exhaustVelocity(Vector3dc posRelBearing, Vector3dc omega) {
        double vel = Math.min(posRelBearing.cross(omega, new Vector3d()).length() * 15, 40);
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