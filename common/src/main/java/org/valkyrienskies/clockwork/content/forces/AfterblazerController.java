package org.valkyrienskies.clockwork.content.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import kotlin.Pair;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerCreateData;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerData;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerUpdateData;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.valkyrienskies.clockwork.content.forces.PropellorController.areQueuesEqual;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AfterblazerController implements ShipForcesInducer {
    private final HashMap<Integer, AfterblazerData> afterblazerData = new HashMap<>();
    private final ConcurrentHashMap<Integer, AfterblazerUpdateData> afterblazerUpdateData = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Pair<Integer, AfterblazerCreateData>> createdJets = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> removedJets = new ConcurrentLinkedQueue<>();
    private int nextJetID = 0;

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdJets.isEmpty()) {
            final Pair<Integer, AfterblazerCreateData> createData = createdJets.remove();
            afterblazerData.put(createData.component1(), new AfterblazerData(
                    createData.component2().pos(),
                    createData.component2().direction(),
                    createData.component2().heat(),
                    createData.component2().gimbal()
            ));
        }
        while (!removedJets.isEmpty()) {
            afterblazerData.remove(removedJets.remove());
        }

        afterblazerUpdateData.forEach((id, data) -> {
            AfterblazerData physData = afterblazerData.get(id);
            if (physData == null) {
                return;
            }
            physData.heat = data.heat();
            physData.gimbal = data.gimbal();
        });

        afterblazerUpdateData.clear();

//        Vector3d netForce = new Vector3d();

        for (AfterblazerData physData: afterblazerData.values()) {
            Pair<Vector3dc, Vector3dc> forceNvec = computeForce(physShip.getTransform(), physData, ((PhysShipImpl) physShip).getPoseVel().getVel(), ((PhysShipImpl) physShip).getPoseVel().getOmega());
            Vector3dc force = forceNvec.component1();
            Vector3dc vec = forceNvec.component2();
            physShip.applyRotDependentForceToPos(force, vec);
//            netForce.add(forceTorque.component1());
        }

//        if (netForce.isFinite()) {
//            physShip.applyInvariantForce(netForce);
//        }

    }

    private Pair<Vector3dc, Vector3dc> computeForce(ShipTransform physTransform, AfterblazerData physJet, Vector3dc vel, Vector3dc omega) {
        Vector3dc jetVector = physTransform.getShipToWorld().transformPosition(physJet.pos.add(0.5,0.5,0.5, new Vector3d()));
        double gimbalX = physJet.gimbal.x();
        double gimbalY = physJet.gimbal.y();

        Vector3dc directionTransformed = physTransform.getShipToWorld().transformDirection(physJet.direction, new Vector3d());

        Vector3dc jetDirection = (physJet.direction).rotateX(gimbalX, new Vector3d()).rotateY(gimbalY);
//        Vector3dc jetDirection = rotatedDir.rotate(jetRotation, new Vector3d());

        Vector3d force = new Vector3d();

        double forceMod = 820000d * ((double) physJet.heat / 5000d);
        Vector3dc jetPosRelCenterMass = physTransform.getShipToWorld().transformPosition(physJet.pos.add(0.5,0.5,0.5, new Vector3d()), new Vector3d()).sub(physTransform.getPositionInWorld(), new Vector3d());
        Vector3dc worldVelAtJet = omega.cross(jetPosRelCenterMass, new Vector3d()).add(vel, new Vector3d());
        double exhaustVel = exhaustVelocity(physJet.heat, false);
        double factor = 1.0 - Mth.clamp(jetDirection.dot(worldVelAtJet) / exhaustVel, 0.0, 1.0);
        if (!Double.isFinite(factor)) {
            factor = 0;
        }

        force.add(jetDirection.mul(forceMod * factor, new Vector3d()));

        return new Pair<>(force,jetVector);
    }

    private double exhaustVelocity(int heat, boolean overYMax) {
        double exhaustVel = heat / 5d;

        if (overYMax) {
            exhaustVel = 2000;
        }
        return exhaustVel;
    }

    public int addAfterblazer(AfterblazerCreateData data) {
        int id = nextJetID++;
        createdJets.add(new Pair<>(id, data));
        return id;
    }

    public void removeAfterblazer(int id) {
        removedJets.add(id);
    }

    public void updateAfterblazer(int id, AfterblazerUpdateData data) {
        afterblazerUpdateData.put(id, data);
    }

    public static AfterblazerController getOrCreate(ServerShip ship) {
        if(ship.getAttachment(AfterblazerController.class) == null) {
            ship.saveAttachment(AfterblazerController.class, new AfterblazerController());
        }
        return ship.getAttachment(AfterblazerController.class);
    }

    public boolean checkAfterblazer(Integer id) {
        if (id != null) {
            return afterblazerData.containsKey(id);
        }
        return false;
    }

    @Override
    public boolean equals(final Object other) {
        // self check
        if (this == other) {
            return true;
        } else if (!(other instanceof final AfterblazerController otherController)) {
            return false;
        } else {
            return Objects.equals(afterblazerData, otherController.afterblazerData)
                    && Objects.equals(afterblazerUpdateData, otherController.afterblazerUpdateData)
                    && areQueuesEqual(createdJets, otherController.createdJets)
                    && areQueuesEqual(removedJets, otherController.removedJets)
                    && nextJetID == otherController.nextJetID;
        }
    }
}
