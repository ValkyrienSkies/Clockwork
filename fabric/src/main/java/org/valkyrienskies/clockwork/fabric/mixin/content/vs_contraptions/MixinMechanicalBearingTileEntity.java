package org.valkyrienskies.clockwork.fabric.mixin.content.vs_contraptions;

import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.clockwork.fabric.mixinducks.CWIControlContraption;

@Mixin(MechanicalBearingTileEntity.class)
public abstract class MixinMechanicalBearingTileEntity implements CWIControlContraption  {

    @Override
    public boolean containsShip() {
        return true;
    }
}
