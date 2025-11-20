package org.valkyrienskies.clockwork.mixin.content.steam_generator;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator.SteamGeneratorBlock;

@Mixin(BoilerData.class)
public class MixinBoilerData {

    @Shadow
    public int attachedEngines;

    @WrapOperation(method = "evaluate", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;"))
    public BlockPos checkForSteamGenerators(BlockPos instance, int dx, int dy, int dz, Operation<BlockPos> original, @Local Level level) {
        final BlockPos pos = original.call(instance, dx, dy, dz);

        for (Direction d : Iterate.directions) {
            BlockPos attachedPos = pos.relative(d);
            BlockState attachedState = level.getBlockState(attachedPos);
            if (ClockworkBlocks.STEAM_GENERATOR.has(attachedState) && SteamGeneratorBlock.getFacing(attachedState) == d)
                attachedEngines++;
        }

        return pos;
    }
}
