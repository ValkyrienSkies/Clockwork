package org.valkyrienskies.clockwork.mixin.content.lubricant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantMaterials;
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;

@Mixin(value = BlockStateInfo.class, remap = false)
public class MixinBlockStateInfo {
    @WrapOperation(method = "onSetBlock*", at = @At(value = "INVOKE", desc =
        @Desc(owner = BlockStateInfo.class, value = "get", args = BlockState.class, ret = Pair.class)))
    private Pair<Double, VsiBlockType> clockwork$coating(BlockStateInfo instance, BlockState state,
            Operation<Pair<Double, VsiBlockType>> original, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y,
            @Local(argsOnly = true, ordinal = 2) int z) {
        return LubricantMaterials.at(level, x, y, z, state, original.call(instance, state));
    }

    @WrapOperation(method = "onSourceAmountChanged", at = @At(value = "INVOKE", desc =
        @Desc(owner = BlockStateInfo.class, value = "get", args = BlockState.class, ret = Pair.class)))
    private Pair<Double, VsiBlockType> clockwork$coatedFluid(BlockStateInfo instance, BlockState state,
            Operation<Pair<Double, VsiBlockType>> original, @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) BlockPos pos) {
        return LubricantMaterials.at(level, pos.getX(), pos.getY(), pos.getZ(), state, original.call(instance, state));
    }

    @Inject(method = "remassShip", at = @At("RETURN"))
    private void clockwork$restoreCoatings(Level level, Ship ship, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && level instanceof ServerLevel serverLevel)
            LubricantData.get(serverLevel).refreshShip(serverLevel, ship);
    }
}
