package org.valkyrienskies.clockwork.mixin.client;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.physicalities.steering.SteeringWheelClient;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void clockwork$steer(boolean slow, float factor, CallbackInfo ci) {
        SteeringWheelClient.capture((KeyboardInput) (Object) this);
    }
}
