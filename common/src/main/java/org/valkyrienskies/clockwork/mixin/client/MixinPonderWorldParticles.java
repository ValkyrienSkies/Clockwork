package org.valkyrienskies.clockwork.mixin.client;

import java.util.Queue;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderWorldParticles;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkModClient;

@Mixin(value = PonderWorldParticles.class, remap = false)
public class MixinPonderWorldParticles {
    @Shadow @Final private Queue<Particle> queue;
    @Shadow PonderLevel world;

    @Inject(method = "clearEffects", at = @At("HEAD"))
    private void clockwork$clearQueuedParticlesOnReplay(CallbackInfo ci) {
        if (ClockworkModClient.getPonderNetworks().containsKey(world)) queue.clear();
    }
}
