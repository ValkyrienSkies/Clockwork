package org.valkyrienskies.clockwork.mixin.content.schematics;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.schematics.VdexCannonAccess;

@Mixin(SchematicannonBlock.class)
public class MixinVdexCannonRemoval {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void clockwork$cancel(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving, CallbackInfo ci) {
        if (oldState.getBlock() == newState.getBlock() || !(level instanceof ServerLevel server)) return;
        if (level.getBlockEntity(pos) instanceof VdexCannonAccess cannon && cannon.getVdexBuild() != null) {
            cannon.getVdexBuild().cancel(server);
            cannon.setVdexBuild(null);
        }
    }
}
