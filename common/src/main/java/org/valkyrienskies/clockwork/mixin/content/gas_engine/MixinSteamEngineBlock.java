package org.valkyrienskies.clockwork.mixin.content.gas_engine;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlock;

@Mixin(SteamEngineBlock.class)
public class MixinSteamEngineBlock {

    @Inject(method = "canAttach", at = @At("RETURN"), cancellable = true, remap = false)
    private static void vs_clockwork$canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection, CallbackInfoReturnable<Boolean> cir) {
        if (pReader.getBlockState(pPos.relative(pDirection)).getBlock() instanceof GasEngineBlock) cir.setReturnValue(Boolean.TRUE);
        else cir.setReturnValue(cir.getReturnValue());
    }

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void vs_clockwork$onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving, CallbackInfo ci) {
        GasEngineBlock.updateEngineState(pLevel, pPos.relative(getFacing(pState).getOpposite()), false);
    }

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void vs_clockwork$onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving, CallbackInfo ci) {
        GasEngineBlock.updateEngineState(pLevel, pPos.relative(getFacing(pState).getOpposite()), true);
    }

    @Shadow
    public static Direction getFacing(BlockState sideState) {
        throw new IllegalStateException();
    }
}
