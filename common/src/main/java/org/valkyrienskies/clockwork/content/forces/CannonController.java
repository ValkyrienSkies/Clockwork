package org.valkyrienskies.clockwork.content.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.contraptions.cannons.CannonCreateData;
import org.valkyrienskies.clockwork.content.contraptions.cannons.CannonData;
import org.valkyrienskies.clockwork.content.contraptions.cannons.CannonUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;

import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.valkyrienskies.clockwork.content.forces.PropellorController.areQueuesEqual;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CannonController implements ShipForcesInducer {

    public final HashMap<Integer, CannonData> cannonData = new HashMap<>();

    private final ConcurrentHashMap<Integer, CannonUpdateData> cannonUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, CannonCreateData>> createdCannons = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedCannons = new ConcurrentLinkedQueue<>();

    private int nextCannonID = 0;

    public static CannonController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(CannonController.class) == null) {
            ship.saveAttachment(CannonController.class, new CannonController());
        }
        return ship.getAttachment(CannonController.class);
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdCannons.isEmpty()) {
            final Pair<Integer, CannonCreateData> createData = createdCannons.remove();
            cannonData.put(createData.left(), new CannonData(
                    createData.right().cannonPos()
            ));
        }
        while (!removedCannons.isEmpty()) {
            cannonData.remove((int) removedCannons.remove());
        }

        cannonUpdateData.forEach((id, data) -> {
            CannonData physData = cannonData.get(id);
            if (physData == null) {
                return;
            }
            physData.recoilVector = data.recoilVector();
        });

        cannonUpdateData.clear();

        for (CannonData data : cannonData.values()) {
            if (data.recoilVector == null) {
                continue;
            }

            Vector3dc force = new Vector3d(data.recoilVector.mul(1, new Vector3d())).mul(1000);

            Vector3dc cannonPosInShip = data.cannonPos.sub(physShip.getTransform().getPositionInShip(), new Vector3d());

            physShip.applyRotDependentForceToPos(force, cannonPosInShip);

            data.recoilVector = null;
        }
    }

    public int addCannon(CannonCreateData data) {
        int id = nextCannonID++;
        createdCannons.add(Pair.of(id, data));
        return id;
    }

    public void removeCannon(int id) {
        removedCannons.add(id);
    }

    public void updateCannon(int id, CannonUpdateData data) {
        cannonUpdateData.put(id, data);
    }

    @Override
    public boolean equals(final Object other) {
        // self check
        if (this == other) {
            return true;
        } else if (!(other instanceof final CannonController otherController)) {
            return false;
        } else {
            return Objects.equals(cannonData, otherController.cannonData)
                    && Objects.equals(cannonUpdateData, otherController.cannonUpdateData)
                    && areQueuesEqual(createdCannons, otherController.createdCannons)
                    && areQueuesEqual(removedCannons, otherController.removedCannons)
                    && nextCannonID == otherController.nextCannonID;
        }
    }
}
