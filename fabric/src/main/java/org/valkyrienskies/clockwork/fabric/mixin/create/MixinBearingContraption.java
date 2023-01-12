package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.fabric.util.propellor.IBearingExtended;

@Mixin(BearingContraption.class)
public class MixinBearingContraption implements IBearingExtended {

    @Inject(
            method = "assemble",
            at = @At(value = "RETURN", ordinal = 1),
            remap = false)
    private void onBlocksEmpty(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) throws AssemblyException {
        failAssembly();
    }

    @Inject(
            method = "assemble",
            at = @At(value = "RETURN", ordinal = 2),
            remap = false)
    private void onSucceededAssemble(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) throws AssemblyException {
        failAssembly();
    }
    private boolean isPropellor = false;


    @Override
    public void setPropellor() {
        isPropellor = true;
    }

    @Shadow(remap = false) protected int sailBlocks;

    private void failAssembly() throws AssemblyException {
        if (isPropellor && sailBlocks < 2) {
            throw new AssemblyException("not_enough_sails", sailBlocks, 2);
        }
    }
}
