package org.valkyrienskies.clockwork.forge.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.interaction.controls.ControlsInputPacket;
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

import java.util.UUID;

@Mixin(ControlsInputPacket.class)
public abstract class MixinControlsInputPacket {
    @Unique
    private Level world;

    @Redirect(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"
            )
    )
    private boolean redirectCloserThan(final Vec3 instance, final Position arg, final double d) {
        Vec3 newVec3 = instance;
        if (VSGameUtilsKt.isBlockInShipyard(this.world, new BlockPos(instance.x, instance.y, instance.z))) {
            final Ship ship = VSGameUtilsKt.getShipManagingPos(this.world, instance);
            newVec3 = VSGameUtilsKt.toWorldCoordinates(ship, instance);
        }
        return newVec3.closerThan(arg, d);
    }

    @Inject(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"
            ), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectCaptureLevel(
            final NetworkEvent.Context ctx, final CallbackInfo ci, final ServerPlayer player, final Level world,
            final UUID uniqueID) {
        this.world = world;
    }
}