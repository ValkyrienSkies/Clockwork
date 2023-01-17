package org.valkyrienskies.clockwork.fabric.util.contraption;

import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionType;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.StabilizedContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.PistonContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyContraption;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraption;
import org.valkyrienskies.clockwork.fabric.ClockWorkModFabric;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.flap.contraption.FlapContraption;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClockworkContraptionTypes {
    public static final ContraptionType
            FLAP = ContraptionType.register(ClockWorkModFabric.asResource("flap").toString(), FlapContraption::new);

    public static void prepare() {}

}
