package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.system.CallbackI;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelCreateData;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelData;
import org.valkyrienskies.clockwork.content.contraptions.reaction_wheel.ReactionWheelUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.physics_api.PoseVel;

import java.lang.Math;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReactionWheelController implements ShipForcesInducer {

    final Int2ObjectOpenHashMap<ReactionWheelData> reactionwheelData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, ReactionWheelUpdateData> reactionwheelUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, ReactionWheelCreateData>> createdRWs = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedRWs = new ConcurrentLinkedQueue<>();

    private int nextRWID = 0;

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
                    createData.right().active()
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
        });

        reactionwheelUpdateData.clear();

//        Vector3d netForce = new Vector3d();
        for (ReactionWheelData physData: reactionwheelData.values()) {

            if (physData.wheelAxis.x() == 1) {
                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().x())) > 0;
            } else if (physData.wheelAxis.y() == 1) {
                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().y())) > 0;
            } else if (physData.wheelAxis.z() == 1) {
                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().z())) > 0;
            }


            if (physData.active) {
                Vector3dc torque = computeTorque(physShip.getTransform(), physData, ((PhysShipImpl) physShip).getInertia().getMomentOfInertiaTensor(), ((PhysShipImpl) physShip).getPoseVel().getOmega(), ((PhysShipImpl) physShip).getPoseVel().getVel(), ((PhysShipImpl) physShip).getInertia().getShipMass());
                physShip.applyInvariantTorque(torque);
            }
//            netForce.add(forceTorque.left());
        }

//        if (netForce.isFinite()) {
//            physShip.applyInvariantForce(netForce);
//        }

    }

    private Vector3dc computeTorque(ShipTransform physTransform, ReactionWheelData physWheel, Matrix3dc inertiaTensor, Vector3dc omega, Vector3dc vel, double mass) {

        Vector3dc wheelPos = new Vector3d(physWheel.wheelPos);
        Vector3dc wheelAxis = new Vector3d(physWheel.wheelAxis).rotate(physTransform.getShipToWorldRotation());
        double wheelSpeed = physWheel.wheelSpeed;



        // 1/2 * Mass * (Outer Wheel Radius^2 + Total Wheel Radius^2)
        double wheelInertia = (0.5*5000) * (Math.pow(0.25, 2) + Math.pow(0.75, 2));

        double rotVel = wheelSpeed * ((2 * Math.PI)/60);
        Vector3dc wheelVel = new Vector3d(wheelAxis).mul(rotVel);
        Vector3d prevAngMomentum = physWheel.prevAngMomentum;
        Vector3dc r = wheelPos.sub(physTransform.getPositionInShip(), new Vector3d());
        Vector3dc momentumModifier = new Vector3d(r).cross(new Vector3d(wheelVel).mul(5000));
        Vector3dc angMomentum = ((wheelAxis.mul(rotVel, new Vector3d()).mul(wheelInertia, new Vector3d()))).add(momentumModifier);

        Vector3dc torque = prevAngMomentum.sub(angMomentum, new Vector3d()).div(1/60.0);
        physWheel.prevAngMomentum = new Vector3d(angMomentum);

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

    public static ReactionWheelController getOrCreate(ServerShip ship) {
        if(ship.getAttachment(ReactionWheelController.class) == null) {
            ship.saveAttachment(ReactionWheelController.class, new ReactionWheelController());
        }
        return ship.getAttachment(ReactionWheelController.class);
    }
}
