package org.valkyrienskies.clockwork.fabric.mixinducks;

import com.simibubi.create.content.contraptions.components.structureMovement.IControlContraption;
import org.valkyrienskies.core.api.ships.Ship;

import javax.annotation.Nullable;

public interface CWIControlContraption extends IControlContraption {

    boolean isShipContraptionController();

    @Nullable
    Ship getConnectedShip();

}
