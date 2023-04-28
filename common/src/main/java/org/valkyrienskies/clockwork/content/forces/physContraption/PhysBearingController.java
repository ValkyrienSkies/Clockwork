package org.valkyrienskies.clockwork.content.forces.physContraption;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingCreateData;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingData;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;

import java.util.HashMap;
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
}
