package org.valkyrienskies.clockwork.mixin.content.gravitron;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronControlsClient;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void clockwork$rotateHeldShip(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        if (!GravitronControlsClient.rotate(yaw, pitch)) original.call(player, yaw, pitch);
    }
}
