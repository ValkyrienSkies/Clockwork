package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.contraptions.flywheel.FlywheelCreateData;
import org.valkyrienskies.clockwork.content.contraptions.flywheel.FlywheelData;
import org.valkyrienskies.clockwork.content.contraptions.flywheel.FlywheelUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FlywheelController implements ShipForcesInducer  {

    final Int2ObjectOpenHashMap<FlywheelData> flywheelData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, FlywheelUpdateData> flywheelUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, FlywheelCreateData>> createdFWs = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedFWs = new ConcurrentLinkedQueue<>();

    private int nextFWID = 0;

    public static FlywheelController getOrCreate(ServerShip ship) {
        if (ship.getAttachment(FlywheelController.class) == null) {
            ship.saveAttachment(FlywheelController.class, new FlywheelController());
        }
        return ship.getAttachment(FlywheelController.class);
    }

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdFWs.isEmpty()) {
            final Pair<Integer, FlywheelCreateData> createData = createdFWs.remove();
            flywheelData.put(createData.left(), new FlywheelData(
                    createData.right().pos(),
                    createData.right().axis(),
                    createData.right().visualSpeed(),
                    createData.right().speed()
            ));
        }
        while (!removedFWs.isEmpty()) {
            flywheelData.remove((int) removedFWs.remove());
        }

        flywheelUpdateData.forEach((id, data) -> {
            FlywheelData physData = flywheelData.get(id);
            if (physData == null) {
                return;
            }
            physData.speed = data.speed();
            physData.visualSpeed = data.visualSpeed();
        });

        flywheelUpdateData.clear();

        for (FlywheelData physData : flywheelData.values()) {
            if (physData.speed != 0) {

                Vector3dc torque = resistRotation(physData, (PhysShipImpl) physShip);

                physShip.applyInvariantTorque(torque);

            }
        }
    }

    private Vector3d resistRotation(FlywheelData physData, PhysShipImpl physShip) {
        double resistance = 0.05;

        float speedMod = Math.abs(physData.speed)/256;

        resistance = resistance * speedMod;

        Vector3d torque = physShip.getPoseVel().getOmega().mul(physShip.getInertia().getMomentOfInertiaTensor(), new Vector3d()).mul(-resistance);

        return torque;
    }

    public int addFlywheel(FlywheelCreateData data) {
        int id = nextFWID++;
        createdFWs.add(Pair.of(id, data));
        return id;
    }

    public void removeFlywheel(int id) {
        removedFWs.add(id);
    }

    public void updateFlywheel(int id, FlywheelUpdateData data) {
        flywheelUpdateData.put(id, data);
    }

}
