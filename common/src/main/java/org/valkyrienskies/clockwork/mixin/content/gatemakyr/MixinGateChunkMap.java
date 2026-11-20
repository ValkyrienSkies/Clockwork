package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrChunkWatch;

@Mixin(ChunkMap.class)
public abstract class MixinGateChunkMap {
    @Shadow @Final private ServerLevel level;
    @Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
    private void clockwork$sendRemoteUpdates(ChunkPos pos, boolean edge, CallbackInfoReturnable<List<ServerPlayer>> cir) {
        List<ServerPlayer> additional = null;
        for (ServerPlayer player : level.players()) {
            if (!GatemakyrChunkWatch.watches(player, pos) || cir.getReturnValue().contains(player)) continue;
            if (additional == null) additional = new ArrayList<>(cir.getReturnValue());
            additional.add(player);
        }
        if (additional != null) cir.setReturnValue(additional);
    }
}
