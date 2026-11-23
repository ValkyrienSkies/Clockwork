package org.valkyrienskies.clockwork.mixin.client;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkConfig;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.util.render.VirtualDuctNetwork;
import org.valkyrienskies.kelvin.api.DuctNodePos;
import org.valkyrienskies.kelvin.util.INodeBlock;
import org.valkyrienskies.kelvin.util.KelvinExtensions;

@Mixin(PonderLevel.class)
public abstract class MixinPonderLevel {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void clockwork$tickDuctNetwork(CallbackInfo ci) {
        PonderLevel level = (PonderLevel) (Object) this;
        VirtualDuctNetwork network = ClockworkModClient.getPonderNetworks().get(level);
        if (network != null) network.tick(level, Math.max(1, ClockworkConfig.KELVIN.getKelvinSubSteps()));
    }

    @Inject(method = "setBlock", at = @At("HEAD"))
    private void clockwork$updateDuctBlock(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        PonderLevel level = (PonderLevel) (Object) this;
        if (level.scene == null) return;
        BlockState old = level.getBlockState(pos);
        if (old == state || !(old.getBlock() instanceof INodeBlock || state.getBlock() instanceof INodeBlock)) return;
        VirtualDuctNetwork network = ClockworkModClient.getKelvin(level);
        if (old.getBlock() != state.getBlock())
            network.removeNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location()));
        network.queueUpdate(pos);
    }

    @Inject(method = "restoreBlocks", at = @At("RETURN"), remap = false)
    private void clockwork$restoreDuctBlocks(Selection selection, CallbackInfo ci) {
        PonderLevel level = (PonderLevel) (Object) this;
        selection.forEach(pos -> {
            DuctNodePos node = KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location());
            VirtualDuctNetwork network = ClockworkModClient.getPonderNetworks().get(level);
            if (level.getBlockState(pos).getBlock() instanceof INodeBlock) {
                network = ClockworkModClient.getKelvin(level);
            } else if (network == null || network.getNodeAt(node) == null) {
                return;
            }
            network.removeNode(node);
            network.queueUpdate(pos);
        });
    }
}
