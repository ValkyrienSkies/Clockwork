package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkTransfer;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.assembly.ShipWelder;

@Mixin(value = ShipWelder.class, remap = false)
public class MixinLinkWelding {
    @Inject(method = "merge", at = @At("RETURN"))
    private void clockwork$moveLinks(ServerLevel level, LoadedServerShip target, BlockPos targetPos, Direction targetFace,
            LoadedServerShip source, BlockPos sourcePos, @Coerce Object plan, CallbackInfoReturnable<String> cir) {
        if (cir.getReturnValue() != null) return;
        WeldPlanAccessor transfer = (WeldPlanAccessor) plan;
        Map<BlockPos, BlockPos> moves = new HashMap<>();
        for (Object entry : transfer.clockwork$getMoves()) {
            WeldMoveAccessor move = (WeldMoveAccessor) entry;
            moves.put(move.clockwork$getFrom(), move.clockwork$getTo());
        }
        WandLinkTransfer.moveRotated(level, moves, transfer.clockwork$getRotation().quaternion());
    }
}
