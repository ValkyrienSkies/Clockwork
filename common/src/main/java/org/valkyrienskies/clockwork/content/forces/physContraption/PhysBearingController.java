package org.valkyrienskies.clockwork.content.forces.physContraption;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingCreateData;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingData;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentOrientationConstraint;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.constraints.VSConstraintKt;
import org.valkyrienskies.core.apigame.constraints.VSHingeTargetAngleConstraint;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PhysBearingController implements ShipForcesInducer {

    public final HashMap<Integer, PhysBearingData> bearingData = new HashMap<>();
    @JsonIgnore
    private final ConcurrentHashMap<Integer, PhysBearingUpdateData> bearingUpdateData = new ConcurrentHashMap<>();
    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Integer, PhysBearingCreateData>> createdBearings = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> removedBearings = new ConcurrentLinkedQueue<>();
    private int nextBearingID = 0;

    public static PhysBearingController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(PhysBearingController.class) == null) {
            ship.saveAttachment(PhysBearingController.class, new PhysBearingController());
        }
        return ship.getAttachment(PhysBearingController.class);
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdBearings.isEmpty()) {
            final Pair<Integer, PhysBearingCreateData> createData = createdBearings.remove();
            bearingData.put(createData.component1(), new PhysBearingData(
                    createData.component2().bearingPos(),
                    createData.component2().bearingAxis(),
                    createData.component2().bearingAngle(),
                    createData.component2().bearingRPM(),
                    createData.component2().locked(),
                    createData.component2().shiptraptionID(),
                    createData.component2().constraint(),
                    createData.component2().hingeConstraint(),
                    createData.component2().posDampConstraint(),
                    createData.component2().rotDampConstraint()
            ));

        }
        while (!removedBearings.isEmpty()) {
            bearingData.remove((int) removedBearings.remove());
        }

        bearingUpdateData.forEach((id, data) -> {
            PhysBearingData physData = bearingData.get(id);
            if (physData == null) {
                return;
            }
            physData.bearingAngle = data.bearingAngle();
            physData.bearingRPM = data.bearingRPM();
            physData.locked = data.locked();
//            physData.hingeConstraint = data.hingeConstraint();
//            physData.angleConstraint = data.angleConstraint();
        });

        bearingUpdateData.clear();

        for (PhysBearingData data : bearingData.values()) {
            if (data.angleConstraint == null) {
                Vector3dc torque = computeRotationalForce(data, (PhysShipImpl) physShip);
                physShip.applyRotDependentTorque(torque);
            }
        }
    }

    private Vector3dc computeRotationalForce(PhysBearingData data, PhysShipImpl physShip) {
        double mass = physShip.getInertia().getShipMass();

        Vector3dc actualOmega = physShip.getPoseVel().getOmega();
        Vector3d idealOmega;
        if (data.bearingAxis != null) {
            idealOmega = data.bearingAxis.mul(data.bearingRPM, new Vector3d()).mul((2*Math.PI)/60);
        } else {
            idealOmega = new Vector3d();
        }


        Vector3dc torque = idealOmega.sub(actualOmega, new Vector3d()).mul(mass * 10);

        return torque;
    }

    public int addPhysBearing(PhysBearingCreateData data) {
        int id = nextBearingID++;
        createdBearings.add(new Pair<>(id, data));
        return id;
    }

    public void removePhysBearing(int id) {
        removedBearings.add(id);
    }

    public void updatePhysBearing(int id, PhysBearingUpdateData data) {
        bearingUpdateData.put(id, data);
    }

    public static <T> boolean areQueuesEqual(final Queue<T> left, final Queue<T> right) {
        return Arrays.equals(left.toArray(), right.toArray());
    }

    @Override
    public boolean equals(final Object other) {
        // self check
        if (this == other) {
            return true;
        } else if (!(other instanceof final PhysBearingController otherController)) {
            return false;
        } else {
            return Objects.equals(bearingData, otherController.bearingData)
                    && Objects.equals(bearingUpdateData, otherController.bearingUpdateData)
                    && areQueuesEqual(createdBearings, otherController.createdBearings)
                    && areQueuesEqual(removedBearings, otherController.removedBearings)
                    && nextBearingID == otherController.nextBearingID;
        }
    }

    public boolean canDisassemble() {
        return false;
    }

    public void setAligning(boolean yn, int id) {
        bearingData.get(id).setAligning(yn);
    }
}
