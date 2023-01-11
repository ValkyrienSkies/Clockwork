package org.valkyrienskies.clockwork.forge.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.interaction.controls.ControlsInputPacket;
import com.simibubi.create.content.logistics.trains.entity.Train;
import com.simibubi.create.content.logistics.trains.entity.TrainRelocationPacket;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.UUID;
import java.util.function.Supplier;

@Mixin(TrainRelocationPacket.class)
public abstract class MixinTrainRelocationPacket {
    @Unique
    private Level world;

    @Redirect(method = "lambda$handle$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean redirectCloserThan(final Vec3 instance, final Position arg, final double d) {
        Vec3 newVec3 = (Vec3) arg;
        final Ship ship = VSGameUtilsKt.getShipManagingPos(this.world, arg);
        if (ship != null) {
            newVec3 = VSGameUtilsKt.toWorldCoordinates(ship, (Vec3) arg);
        }
        return instance.closerThan(newVec3, d);
    }

    @Inject(method = "lambda$handle$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectCaptureLevel(NetworkEvent.Context ctx, CallbackInfo ci, ServerPlayer sender, Train train) {
        this.world = sender.level;
    }
}