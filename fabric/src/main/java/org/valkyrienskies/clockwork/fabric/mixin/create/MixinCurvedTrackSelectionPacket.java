package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.content.logistics.trains.management.edgePoint.CurvedTrackSelectionPacket;
import com.simibubi.create.content.logistics.trains.track.TrackTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CurvedTrackSelectionPacket.class)
public abstract class MixinCurvedTrackSelectionPacket {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.MixinCurvedTrackSelectionPacket");

    @Shadow
    private BlockPos targetPos;
    @Shadow
    private boolean front;
    @Shadow
    private int segment;
    @Shadow
    private int slot;

    @Inject(
        method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/logistics/trains/track/TrackTileEntity;)V",
        at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void inInject(final ServerPlayer player, final TrackTileEntity te, final CallbackInfo ci) {
        LOGGER.warn("applySettings targetPos " + this.targetPos);
    }

    @Inject(
        method = "readSettings",
        at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injectReadBlockPos(final FriendlyByteBuf buffer, final CallbackInfo ci) {
        LOGGER.warn("readSettings targetPos " + this.targetPos);
    }

    @Inject(
        method = "writeSettings",
        at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void redirectWriteBlockPos(final FriendlyByteBuf buffer, final CallbackInfo ci) {
        LOGGER.warn("writeSettings targetPos " + this.targetPos);
    }
}
