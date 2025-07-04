package org.valkyrienskies.clockwork.mixin.content.gas_engine;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlock;

@Mixin(SteamEngineBlock.class)
public class MixinSteamEngineBlock {

    @Inject(method = "canAttach", at = @At("RETURN"), cancellable = true)
    private static void vs_clockwork$canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockpos = pPos.relative(pDirection);
        if (pReader.getBlockState(blockpos).getBlock() instanceof GasEngineBlock) {
            cir.setReturnValue(Boolean.TRUE);
        }
        cir.setReturnValue(cir.getReturnValue());
    }
}
