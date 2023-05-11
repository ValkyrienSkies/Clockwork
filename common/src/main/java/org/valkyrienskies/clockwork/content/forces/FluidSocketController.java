package org.valkyrienskies.clockwork.content.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.tubing.FluidSocketCreateData;
import org.valkyrienskies.clockwork.content.contraptions.tubing.FluidSocketData;
import org.valkyrienskies.clockwork.content.contraptions.tubing.FluidSocketUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FluidSocketController implements ShipForcesInducer {

    public final HashMap<Integer, FluidSocketData> socketData = new HashMap<>();
    @JsonIgnore
    private final ConcurrentHashMap<Integer, FluidSocketUpdateData> socketUpdateData = new ConcurrentHashMap<>();
    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Integer, FluidSocketCreateData>> createdSockets = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> removedSockets = new ConcurrentLinkedQueue<>();
    private int nextsocketID = 0;

    public static FluidSocketController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(FluidSocketController.class) == null) {
            ship.saveAttachment(FluidSocketController.class, new FluidSocketController());
        }
        return ship.getAttachment(FluidSocketController.class);
    }
    
    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdSockets.isEmpty()) {
            final Pair<Integer, FluidSocketCreateData> createData = createdSockets.remove();
            socketData.put(createData.component1(), new FluidSocketData(
                    createData.component2().connectedPos()
            ));

        }
        while (!removedSockets.isEmpty()) {
            socketData.remove((int) removedSockets.remove());
        }

        socketUpdateData.forEach((id, data) -> {
            FluidSocketData physData = socketData.get(id);
            if (physData == null) {
                return;
            }
            physData.constraint = data.constraint();
            physData.constraintID = data.constraintID();
        });

        socketUpdateData.clear();
    }

    public int addFluidSocket(FluidSocketCreateData data) {
        int id = nextsocketID++;
        createdSockets.add(new Pair<>(id, data));
        return id;
    }

    public void removeFluidSocket(int id) {
        removedSockets.add(id);
    }

    public void updateFluidSocket(int id, FluidSocketUpdateData data) {
        socketUpdateData.put(id, data);
    }

    public static <T> boolean areQueuesEqual(final Queue<T> left, final Queue<T> right) {
        return Arrays.equals(left.toArray(), right.toArray());
    }

    @Override
    public boolean equals(final Object other) {
        // self check
        if (this == other) {
            return true;
        } else if (!(other instanceof final FluidSocketController otherController)) {
            return false;
        } else {
            return Objects.equals(socketData, otherController.socketData)
                    && Objects.equals(socketUpdateData, otherController.socketUpdateData)
                    && areQueuesEqual(createdSockets, otherController.createdSockets)
                    && areQueuesEqual(removedSockets, otherController.removedSockets)
                    && nextsocketID == otherController.nextsocketID;
        }
    }
}
