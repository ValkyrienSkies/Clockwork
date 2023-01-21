package org.valkyrienskies.clockwork.mixinduck;

import com.simibubi.create.content.contraptions.components.fan.IAirCurrentSource;
import org.valkyrienskies.core.api.ships.Ship;

public interface IExtendedAirCurrentSource extends IAirCurrentSource {

    Ship getShip();

}
