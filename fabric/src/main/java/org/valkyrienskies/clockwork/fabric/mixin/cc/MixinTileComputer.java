package org.valkyrienskies.clockwork.fabric.mixin.cc;

import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.integration.cc.ClockworkComputerCraftIntegration;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Pseudo
@Mixin(TileComputer.class)
public class MixinTileComputer {
    @Inject(method = "createComputer", at = @At("RETURN"), remap = false)
    public void clockwork$addAPIs(int instanceID, int id, CallbackInfoReturnable<ServerComputer> cir) {
        ServerComputer computer = cir.getReturnValue();
        ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) computer.getWorld(), computer.getPosition());

        ClockworkComputerCraftIntegration.INSTANCE.addAPIs(computer, ship);
    }
}