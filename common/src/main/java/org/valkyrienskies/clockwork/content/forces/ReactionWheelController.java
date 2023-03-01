package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelCreateData;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelData;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReactionWheelController implements ShipForcesInducer {

    final Int2ObjectOpenHashMap<ReactionWheelData> reactionwheelData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, ReactionWheelUpdateData> reactionwheelUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, ReactionWheelCreateData>> createdRWs = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedRWs = new ConcurrentLinkedQueue<>();

    private int nextRWID = 0;

    public static ReactionWheelController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(ReactionWheelController.class) == null) {
            ship.saveAttachment(ReactionWheelController.class, new ReactionWheelController());
        }
        return ship.getAttachment(ReactionWheelController.class);
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdRWs.isEmpty()) {
            final Pair<Integer, ReactionWheelCreateData> createData = createdRWs.remove();
            reactionwheelData.put(createData.left(), new ReactionWheelData(
                createData.right().wheelPos(),
                createData.right().wheelAxis(),
                createData.right().wheelSpeed(),
                createData.right().spinup(),
                createData.right().spindown(),
                createData.right().active(),
                createData.right().sourceSpeed()
            ));
        }
        while (!removedRWs.isEmpty()) {
            reactionwheelData.remove((int) removedRWs.remove());
        }

        reactionwheelUpdateData.forEach((id, data) -> {
            ReactionWheelData physData = reactionwheelData.get(id);
            if (physData == null) {
                return;
            }
            physData.wheelSpeed = data.speed();
            physData.sourceSpeed = data.sourceSpeed();
        });

        reactionwheelUpdateData.clear();

        for (ReactionWheelData physData : reactionwheelData.values()) {
//            if (physData.wheelAxis.x() == 1) {
//                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().x())) >= 10;
//            } else if (physData.wheelAxis.y() == 1) {
//                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().y())) >= 10;
//            } else if (physData.wheelAxis.z() == 1) {
//                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().z())) >= 10;
//            }
            //FOR TESTING
            physData.active = true;


            if (physData.sourceSpeed != 0) {
                if (physData.active) {

                    Vector3dc torque = computeTorque(physShip.getTransform(), physData, ((PhysShipImpl) physShip).getPoseVel().getOmega(), ((PhysShipImpl) physShip));
//                    Vector3dc troque = computeResistance(((PhysShipImpl) physShip), physData);
                    if (torque.isFinite()) {
                        physShip.applyInvariantTorque(torque);
                    }
                }
            }
        }
    }

    private Vector3dc computeTorque(ShipTransform physTransform, ReactionWheelData physWheel, Vector3dc omega, PhysShipImpl physShip) {
        Vector3dc prevAngMomentumRelWheel = physWheel.prevAngMomentum;
        Vector3dc wheelPos = physWheel.wheelPos;

        Vector3dc wheelAxis = new Vector3d(physWheel.wheelAxis);
        double wheelSpeed = physWheel.wheelSpeed;


        double wheelMass = 18000;

        // 1/2 * Mass * (Outer Wheel Radius^2 + Total Wheel Radius^2)
        double wheelInertia = (0.5 * wheelMass) * (Math.pow(0.25, 2) + Math.pow(0.75, 2));

        double rotVel = wheelSpeed * ((2 * Math.PI) / 60);

        Vector3dc r = new Vector3d(wheelPos).sub(physTransform.getPositionInShip()).rotate(physTransform.getShipToWorldRotation());
        Vector3dc angularMomentumRelWheel = new Vector3d(wheelAxis).mul(rotVel).mul(wheelInertia);

        // Add to convert from momentum relative to wheel into relative to ship
        Vector3dc momentumModifier = new Vector3d(omega).cross(r).mul(wheelMass);

        Vector3dc angularMomentumRelShip = new Vector3d(angularMomentumRelWheel).add(momentumModifier);
        Vector3dc prevAngularMomentumRelShip = new Vector3d(prevAngMomentumRelWheel).add(momentumModifier);

        Vector3dc torque = new Vector3d(prevAngularMomentumRelShip).sub(angularMomentumRelShip).div(1 / 60.0);

        physWheel.prevAngMomentum = angularMomentumRelWheel;

        return torque;
    }

    public int addReactionWheel(ReactionWheelCreateData data) {
        int id = nextRWID++;
        createdRWs.add(Pair.of(id, data));
        return id;
    }

    public void removeReactionWheel(int id) {
        removedRWs.add(id);
    }

    public void updateReactionWheel(int id, ReactionWheelUpdateData data) {
        reactionwheelUpdateData.put(id, data);
    }
}
