package org.valkyrienskies.clockwork.forge.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.forge.util.propellor.IBearingExtended;

@Mixin(BearingContraption.class)
public class MixinBearingContraption implements IBearingExtended {

    @Shadow(remap = false)
    protected int sailBlocks;
    private boolean isPropellor = false;

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

    @Override
    public void setPropellor() {
        isPropellor = true;
    }

    private void failAssembly() throws AssemblyException {
        if (isPropellor && sailBlocks < 2) {
            throw new AssemblyException("not_enough_sails", sailBlocks, 2);
        }
    }
}
