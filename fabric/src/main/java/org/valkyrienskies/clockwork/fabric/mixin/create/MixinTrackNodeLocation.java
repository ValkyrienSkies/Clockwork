package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.content.logistics.trains.DimensionPalette;
import com.simibubi.create.content.logistics.trains.TrackNodeLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TrackNodeLocation.class)
public abstract class MixinTrackNodeLocation {
    @Shadow
    public abstract Vec3 getLocation();

    @Shadow
    public ResourceKey<Level> dimension;

    @Shadow
    private static TrackNodeLocation fromPackedPos(final BlockPos bufferPos) {
        LOGGER.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!Shadow didn't shadow!!!!!!!!!!!!!!!!!!");
        return new TrackNodeLocation(0, 0, 0);
    }

    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.MixinTrackNodeLocation");

    @Unique
    public Level level;

    /**
     * This method overwrites getLocation to make it actually parse things as a double, not needed in forge create
     */
    @Inject(
        method = "getLocation", at = @At("HEAD"), cancellable = true
    )
    protected void getLocation(final CallbackInfoReturnable<Vec3> cir) {

        cir.setReturnValue(new Vec3((double) ((TrackNodeLocation) (Object) this).getX() / 2,
            (double) ((TrackNodeLocation) (Object) this).getY() / 2,
            (double) ((TrackNodeLocation) (Object) this).getZ() / 2));
    }

    @Inject(
        method = "in(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/logistics/trains/TrackNodeLocation;",
        at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void inInject(final Level level, final CallbackInfoReturnable<TrackNodeLocation> cir) {
        this.level = level;
    }

    @Inject(
        method = "read", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void readInject(final CompoundTag tag, final DimensionPalette dimensions,
        final CallbackInfoReturnable<TrackNodeLocation> cir, final TrackNodeLocation location) {
        LOGGER.warn("Reading TrackNodeLocation " + location);
    }

    @Inject(
        method = "send", at = @At("HEAD"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true
    )
    private void sendInject(final FriendlyByteBuf buffer, final DimensionPalette dimensions, final CallbackInfo ci) {
        final Vec3 loc = this.getLocation();

        final BlockPos blockPos = new BlockPos(((TrackNodeLocation) (Object) this));
        final long longBlockPos = blockPos.asLong();
        LOGGER.warn(
            "Send TrackNodeLocation " + this + " BlockPos " + blockPos + " getLocation() " +
                this.getLocation() + " blockPos as long " + longBlockPos + " fromLong " + BlockPos.of(longBlockPos));
        /*
        //buffer.writeBlockPos(new BlockPos(((TrackNodeLocation) (Object) this)));
        buffer.writeDouble(((TrackNodeLocation) (Object) this).getX());
        buffer.writeDouble(((TrackNodeLocation) (Object) this).getY());
        buffer.writeDouble(((TrackNodeLocation) (Object) this).getZ());
        buffer.writeVarInt(dimensions.encode(((TrackNodeLocation) (Object) this).dimension));

        LOGGER.warn("Hopefully written to buffer");
        ci.cancel();*/
    }

    @Inject(
        method = "receive", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true
    )
    private static void receiveInject(final FriendlyByteBuf buffer, final DimensionPalette dimensions,
        final CallbackInfoReturnable<TrackNodeLocation> cir, final TrackNodeLocation location) {
        /*
        final double x = buffer.readDouble();
        final double y = buffer.readDouble();
        final double z = buffer.readDouble();
        //LOGGER.warn("Receiving Assembling " + x + "," + y + "," + z);
        final TrackNodeLocation location = fromPackedPos(new BlockPos(x, y, z));
        location.dimension = dimensions.decode(buffer.readVarInt());
        */
        LOGGER.warn("Receiving TrackNodeLocation " + location);
        //cir.setReturnValue(location);
    }
}
