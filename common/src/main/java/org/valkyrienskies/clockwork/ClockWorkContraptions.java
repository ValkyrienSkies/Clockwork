package org.valkyrienskies.clockwork;

import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionType;
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorContraption;

public class ClockWorkContraptions {
    public static final ContraptionType
            FLAP = ContraptionType.register(ClockWorkMod.asResource("flap").toString(), FlapContraption::new),
            PROPELLOR = ContraptionType.register(ClockWorkMod.asResource("propellor").toString(), PropellorContraption::new);

    public static void init() {
    }

}
