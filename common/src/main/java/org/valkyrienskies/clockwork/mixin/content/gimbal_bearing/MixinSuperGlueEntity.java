package org.valkyrienskies.clockwork.mixin.content.gimbal_bearing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkBlocks;

@Mixin(SuperGlueEntity.class)
public class MixinSuperGlueEntity {
    @Inject(
            method = "isSideSticky",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private static void isSideSticky(Level world, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = world.getBlockState(pos);
        if (ClockworkBlocks.GIMBAL_BEARING.has(state)) {
            cir.setReturnValue(state.getValue(DirectionalKineticBlock.FACING) == direction);
            cir.cancel();
        }
        if (ClockworkBlocks.PHYS_BEARING.has(state)) {
            cir.setReturnValue(state.getValue(DirectionalKineticBlock.FACING) == direction);
            cir.cancel();
        }
    }
}
