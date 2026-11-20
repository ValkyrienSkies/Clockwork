package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(value = VSGameUtilsKt.class, remap = false)
public abstract class MixinGateShipMenuDistance {
    @ModifyReturnValue(method = "squaredDistanceToInclShips", at = @At("RETURN"), require = 0)
    private static double clockwork$gateEntityMenuDistance(double original, Entity entity, double x, double y, double z) {
        if (entity instanceof ServerPlayer player && player == GatemakyrBlockInteraction.menuPlayer())
            return GatemakyrBlockInteraction.menuDistance(original, player, x, y, z);
        return original;
    }

    @ModifyReturnValue(method = "squaredDistanceToPhysicalBlock", at = @At("RETURN"), require = 0)
    private static double clockwork$gateMenuDistance(double original, Level level, BlockPos pos, Vec3 observer) {
        ServerPlayer player = GatemakyrBlockInteraction.menuPlayer();
        if (player != null && player.level() == level)
            return GatemakyrBlockInteraction.menuDistance(original, player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return original;
    }
}
