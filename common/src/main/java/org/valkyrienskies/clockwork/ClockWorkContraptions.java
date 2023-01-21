package org.valkyrienskies.clockwork;

import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionType;
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption;

public class ClockWorkContraptions {
    public static final ContraptionType
            FLAP = ContraptionType.register(ClockWorkMod.asResource("flap").toString(), FlapContraption::new);

    public static void init() {
    }

}
