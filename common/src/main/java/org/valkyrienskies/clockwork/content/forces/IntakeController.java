package org.valkyrienskies.clockwork.content.forces;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.intake.IntakeCreateData;
import org.valkyrienskies.clockwork.content.contraptions.intake.IntakeData;
import org.valkyrienskies.clockwork.content.contraptions.intake.IntakeUpdateData;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ShipForcesInducer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IntakeController implements ShipForcesInducer {

    final Int2ObjectOpenHashMap<IntakeData> intakeData = new Int2ObjectOpenHashMap<>();

    private final ConcurrentHashMap<Integer, IntakeUpdateData> intakeUpdateData = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Pair<Integer, IntakeCreateData>> createdIntakes = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Integer> removedIntakes = new ConcurrentLinkedQueue<>();

    private int nextIntakeID = 0;

    @Override
    public void applyForces(@NotNull PhysShip physShip) {
        while (!createdIntakes.isEmpty()) {
            final Pair<Integer, IntakeCreateData> createData = createdIntakes.remove();
            intakeData.put(createData.left(), new IntakeData(
                    createData.right().intakeSpeed(),
                    createData.right().intakePos()
            ));
        }
        while (!removedIntakes.isEmpty()) {
            intakeData.remove((int) removedIntakes.remove());
        }

        intakeUpdateData.forEach((id, data) -> {
            IntakeData physData = intakeData.get(id);
            if (physData == null) {
                return;
            }
            physData.intakeSpeed = data.intakeSpeed();
        });
        intakeUpdateData.clear();


    }

    public int addIntake(IntakeCreateData data) {
        int id = nextIntakeID++;
        createdIntakes.add(Pair.of(id, data));
        return id;
    }

    public void removeIntake(int id) {
        removedIntakes.add(id);
    }

    public void updateIntake(int id, IntakeUpdateData data) {
        intakeUpdateData.put(id, data);
    }

    public static IntakeController getOrCreate(ServerShip ship) {
        if(ship.getAttachment(IntakeController.class) == null) {
            ship.saveAttachment(IntakeController.class, new IntakeController());
        }
        return ship.getAttachment(IntakeController.class);
    }

}
