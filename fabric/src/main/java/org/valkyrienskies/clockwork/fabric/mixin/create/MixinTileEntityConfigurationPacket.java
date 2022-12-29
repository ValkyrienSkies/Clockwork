package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.foundation.networking.TileEntityConfigurationPacket;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(TileEntityConfigurationPacket.class)
public abstract class MixinTileEntityConfigurationPacket {
    @Unique
    private Level world;

    @Redirect(
        method = "lambda$handle$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"
        )
    )
    private boolean redirectCloserThan(final BlockPos instance, final Vec3i vec3i, final double v) {
        BlockPos blockPos = instance;
        if (VSGameUtilsKt.isBlockInShipyard(this.world, instance)) {
            final Ship ship = VSGameUtilsKt.getShipManagingPos(this.world, instance);
            final Vector3d tempVec = VSGameUtilsKt.toWorldCoordinates(ship, instance);
            blockPos = new BlockPos(tempVec.x, tempVec.y, tempVec.z);
        }
        return blockPos.closerThan(vec3i, v);
    }

    @Inject(
        method = "lambda$handle$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z"
        ), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectCaptureLevel(
        final Supplier context, final CallbackInfo ci, final ServerPlayer player, final Level world) {
        this.world = world;
    }
}