package org.valkyrienskies.clockwork.content.forces.physContraption;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingCreateData;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingData;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PhysBearingController implements ShipForcesInducer {

    public final HashMap<Integer, PhysBearingData> bearingData = new HashMap<>();
    private final ConcurrentHashMap<Integer, PhysBearingUpdateData> bearingUpdateData = new ConcurrentHashMap<>();
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
}
