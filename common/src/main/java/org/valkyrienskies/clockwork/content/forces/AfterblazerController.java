package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerCreateData;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerData;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerUpdateData;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.lang.Math;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AfterblazerController implements ShipForcesInducer {

    final Int2ObjectOpenHashMap<AfterblazerData> afterblazerData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, AfterblazerUpdateData> afterblazerUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, AfterblazerCreateData>> createdJets = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedJets = new ConcurrentLinkedQueue<>();

    private int nextJetID = 0;

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdJets.isEmpty()) {
            final Pair<Integer, AfterblazerCreateData> createData = createdJets.remove();
            afterblazerData.put(createData.left(), new AfterblazerData(
                    createData.right().jetDirection(),
                    createData.right().jetBurnTime(),
                    createData.right().heatLevel(),
                    createData.right().redstoneLevel(),
                    createData.right().jetPos(),
                    createData.right().jetGimbal()
            ));
        }
        while (!removedJets.isEmpty()) {
            afterblazerData.remove((int) removedJets.remove());
        }

        afterblazerUpdateData.forEach((id, data) -> {
            AfterblazerData physData = afterblazerData.get(id);
            if (physData == null) {
                return;
            }
            physData.jetBurnTime = data.jetBurnTime();
            physData.heatLevel = data.heatLevel();
            physData.redstoneLevel = data.redstoneLevel();
        });

        afterblazerUpdateData.clear();

//        Vector3d netForce = new Vector3d();

        for (AfterblazerData physData: afterblazerData.values()) {
            Vector3dc force = computeForce(physShip.getTransform(), physData, ((PhysShipImpl) physShip).getPoseVel().getVel(), ((PhysShipImpl) physShip).getPoseVel().getOmega());
            physShip.applyRotDependentForceToPos(force, physShip.getTransform().getShipToWorld().transformPosition(physData.jetPos, new Vector3d()));
//            netForce.add(forceTorque.left());
        }

//        if (netForce.isFinite()) {
//            physShip.applyInvariantForce(netForce);
//        }

    }

    private Vector3dc computeForce(ShipTransform physTransform, AfterblazerData physJet, Vector3dc vel, Vector3dc omega) {
        Vector3dc jetVector = physTransform.getShipToWorld().transformPosition(new Vector3d(physJet.jetPos).add(0.5, 0.5, 0.5));
        double gimbalX = physJet.jetGimbal.x;
        double gimbalY = physJet.jetGimbal.y;
        double throttle = physJet.redstoneLevel/15f;
        double multiplier = switch (physJet.heatLevel) {
            case INFURIATED -> 3;
            case SEETHING -> 1.5;
            case KINDLED -> 1;
            case FADING -> 0.5;
            default -> 0;
        };

        Quaterniondc jetRotation = switch (physJet.jetDirection) {
            case DOWN -> new Quaterniond().rotateX(Math.toRadians(90));
            case UP -> new Quaterniond().rotateX(Math.toRadians(270));
            case NORTH -> new Quaterniond();
            case SOUTH -> new Quaterniond().rotateY(Math.toRadians(180));
            case WEST -> new Quaterniond().rotateY(Math.toRadians(270));
            case EAST -> new Quaterniond().rotateY(Math.toRadians(90));
        };

        Vector3dc rotatedDir = (new Vector3d(0,0,-1)).rotateX(gimbalX).rotateY(gimbalY);
        Vector3dc jetDirection = rotatedDir.rotate(jetRotation, new Vector3d());

        Vector3d force = new Vector3d();

        double forceMod = 40000 * throttle * multiplier;

        double exhaustVel = exhaustVelocity(multiplier, new Vector3d(vel));
        double forceLocker = 1 - Mth.clamp(100 / exhaustVel, 0, 1);
        if (!Double.isFinite(forceLocker)) {
            forceLocker = 0;
        }

        force.add(jetDirection.mul(forceMod * forceLocker, new Vector3d()));

        return force;
    }

    private double exhaustVelocity(double multiplier, Vector3d velocity) {
        double vel = 100 * multiplier;
        double vel2 = velocity.length();
        return Math.sqrt(vel * vel + vel2 * vel2);
    }

    public int addAfterblazer(AfterblazerCreateData data) {
        int id = nextJetID++;
        createdJets.add(Pair.of(id, data));
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
}
