package org.valkyrienskies.clockwork.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.util.BlockUpdateCollector;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;

@Mixin(LevelChunk.class)
public class MixinBlockStateInfoProvider {

    @Final
    @Shadow
    Level level;

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void vs_clockwork$postSetBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        if (this.level instanceof ServerLevel serverLevel) {
            BlockUpdateCollector.INSTANCE.onSetBlock(serverLevel, pos, state);
        }
    }
}
