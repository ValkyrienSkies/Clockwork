package org.valkyrienskies.clockwork.content.contraptions.intake;

import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.List;

public class IntakeData {
    public final Vector3dc intakePos;
    public double intakeSpeed;

    public IntakeData(double intakeSpeed, Vector3dc intakePos) {
        this.intakePos = intakePos;
        this.intakeSpeed = intakeSpeed;
    }
}
