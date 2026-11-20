package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;

@Mixin({Player.class, ServerPlayer.class, ServerGamePacketListenerImpl.class})
public abstract class MixinGateMenu {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean clockwork$checkGateMenu(AbstractContainerMenu menu, Player player, Operation<Boolean> original) {
        return GatemakyrBlockInteraction.checkMenu(menu, player, () -> original.call(menu, player));
    }
}
