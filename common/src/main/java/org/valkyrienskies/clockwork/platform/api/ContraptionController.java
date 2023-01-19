package org.valkyrienskies.clockwork.platform.api;

import com.simibubi.create.content.contraptions.components.structureMovement.IControlContraption;
import org.valkyrienskies.core.api.ships.Ship;

import javax.annotation.Nullable;

public interface ContraptionController extends IControlContraption {
    boolean isShipContraptionController();

    @Nullable
    Ship getConnectedShip();
}
