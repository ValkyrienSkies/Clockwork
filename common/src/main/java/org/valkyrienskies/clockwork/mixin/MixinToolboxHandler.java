package org.valkyrienskies.clockwork.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.ClockworkItems;


@Mixin(ToolboxHandler.class)
public class MixinToolboxHandler {

    @WrapOperation(method = "getNearest", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/equipment/toolbox/ToolboxHandler;getMaxRange(Lnet/minecraft/world/entity/player/Player;)D"))
    private static double vs_clockwork$$cancelToolbox(Player player, Operation<Double> original) {
        if (player.getMainHandItem().is(ClockworkItems.GRAVITRON.get().asItem())) {
            return 0.0;
        }
        return original.call(player);
    }
}
