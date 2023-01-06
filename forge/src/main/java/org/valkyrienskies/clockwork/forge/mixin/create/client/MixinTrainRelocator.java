package org.valkyrienskies.clockwork.forge.mixin.create.client;

import com.simibubi.create.content.logistics.trains.entity.Train;
import com.simibubi.create.content.logistics.trains.entity.TrainRelocationPacket;
import com.simibubi.create.content.logistics.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(TrainRelocator.class)
public abstract class MixinTrainRelocator {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean redirectCloserThan(final Vec3 instance, final Position arg, final double d) {
        Vec3 newVec3 = (Vec3) arg;
        Level world = Minecraft.getInstance().player.level;
        final Ship ship = VSGameUtilsKt.getShipManagingPos(world, arg);
        if (ship != null) {
            newVec3 = VSGameUtilsKt.toWorldCoordinates(ship, (Vec3) arg);
        }
        return instance.closerThan(newVec3, d);
    }
}