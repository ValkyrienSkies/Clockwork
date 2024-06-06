package org.valkyrienskies.clockwork.fabric.mixin.cc;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.integration.cc.ClockworkComputerCraftIntegration;

@Pseudo
@Mixin(TileTurtle.class)
public class MixinTileTurtle {
    @Inject(method = "createComputer", at = @At("RETURN"), cancellable = true)
    public void clockwork$addAPIs(int instanceID, int id, CallbackInfoReturnable<ServerComputer> cir) {
        ServerComputer computer = cir.getReturnValue();
        ClockworkComputerCraftIntegration.INSTANCE.addAPIs((ServerLevel) computer.getWorld(), computer);
        cir.setReturnValue(computer);
    }
}
