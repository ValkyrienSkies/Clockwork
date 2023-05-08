package org.valkyrienskies.clockwork.content.forces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointCreateData;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointData;
import org.valkyrienskies.clockwork.content.contraptions.universal_joint.UniversalJointUpdateData;
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
public class UniversalJointController implements ShipForcesInducer {

    public final HashMap<Integer, UniversalJointData> jointData = new HashMap<>();
    @JsonIgnore
    private final ConcurrentHashMap<Integer, UniversalJointUpdateData> jointUpdateData = new ConcurrentHashMap<>();
    @JsonIgnore
    private final ConcurrentLinkedQueue<Pair<Integer, UniversalJointCreateData>> createdjoints = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> removedjoints = new ConcurrentLinkedQueue<>();
    private int nextjointID = 0;

    public static UniversalJointController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(UniversalJointController.class) == null) {
            ship.saveAttachment(UniversalJointController.class, new UniversalJointController());
        }
        return ship.getAttachment(UniversalJointController.class);
    }
    
    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdjoints.isEmpty()) {
            final Pair<Integer, UniversalJointCreateData> createData = createdjoints.remove();
            jointData.put(createData.component1(), new UniversalJointData(
                    createData.component2().connectedPos()
            ));

        }
        while (!removedjoints.isEmpty()) {
            jointData.remove((int) removedjoints.remove());
        }

        jointUpdateData.forEach((id, data) -> {
            UniversalJointData physData = jointData.get(id);
            if (physData == null) {
                return;
            }
            physData.constraint = data.constraint();
            physData.constraintID = data.constraintID();
        });

        jointUpdateData.clear();
    }

    public int addUniversalJoint(UniversalJointCreateData data) {
        int id = nextjointID++;
        createdjoints.add(new Pair<>(id, data));
        return id;
    }

    public void removeUniversalJoint(int id) {
        removedjoints.add(id);
    }

    public void updateUniversalJoint(int id, UniversalJointUpdateData data) {
        jointUpdateData.put(id, data);
    }

    public static <T> boolean areQueuesEqual(final Queue<T> left, final Queue<T> right) {
        return Arrays.equals(left.toArray(), right.toArray());
    }

    @Override
    public boolean equals(final Object other) {
        // self check
        if (this == other) {
            return true;
        } else if (!(other instanceof final UniversalJointController otherController)) {
            return false;
        } else {
            return Objects.equals(jointData, otherController.jointData)
                    && Objects.equals(jointUpdateData, otherController.jointUpdateData)
                    && areQueuesEqual(createdjoints, otherController.createdjoints)
                    && areQueuesEqual(removedjoints, otherController.removedjoints)
                    && nextjointID == otherController.nextjointID;
        }
    }
}
