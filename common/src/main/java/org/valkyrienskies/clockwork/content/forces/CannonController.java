package org.valkyrienskies.clockwork.content.forces;

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

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CannonController implements ShipForcesInducer {

    public final Int2ObjectOpenHashMap<CannonData> cannonData = new Int2ObjectOpenHashMap<>();

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
            physData.recoilForce = data.recoilForce();
        });

        cannonUpdateData.clear();

        for (CannonData data : cannonData.values()) {
            if (data.recoilForce == 0) {
                continue;
            }
            Vector3dc force = new Vector3d(data.recoilVector.mul(data.recoilForce, new Vector3d())).mul(1000);

            Vector3dc cannonPosInShip = data.cannonPos.sub(physShip.getTransform().getPositionInShip(), new Vector3d());

            physShip.applyRotDependentForceToPos(force, cannonPosInShip);

            data.recoilForce = 0;
            data.recoilVector = new Vector3d();
        }
    }

    public int addCannon(CannonCreateData data) {
        int id = nextCannonID++;
        createdCannons.add(Pair.of(id, data));
        return id;
    }

    public void removeBalloon(int id) {
        removedCannons.add(id);
    }

    public void updateBalloon(int id, CannonUpdateData data) {
        cannonUpdateData.put(id, data);
    }
}
