package org.valkyrienskies.clockwork.mixin.accessors;

import com.simibubi.create.content.contraptions.components.structureMovement.piston.LinearActuatorTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.PistonContraption;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PistonContraption.class)
public interface IMixinPistonContraption {
    @Accessor Direction getOrientation();
}
