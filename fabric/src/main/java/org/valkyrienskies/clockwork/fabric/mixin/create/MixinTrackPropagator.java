package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.trains.TrackGraph;
import com.simibubi.create.content.logistics.trains.TrackNode;
import com.simibubi.create.content.logistics.trains.TrackNodeLocation;
import com.simibubi.create.content.logistics.trains.TrackPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TrackPropagator.class)
public class MixinTrackPropagator {

    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.client.MixinTrackPropagator");

    @Inject(
        method = "onRailAdded", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void injector(final LevelAccessor reader, final BlockPos pos, final BlockState state,
        final CallbackInfoReturnable<TrackGraph> cir) {
        LOGGER.warn("Level = " + reader + "      pos = " + pos);
        final TrackNodeLocation targetLoc =
            new TrackNodeLocation(new Vec3(pos.getX(), pos.getY(), pos.getZ())).in((Level) reader);

        for (final TrackGraph railGraph : Create.RAILWAYS.sided(reader).trackNetworks.values()) {
            LOGGER.warn("RailGraphLoop2");
            railGraph.getNodes().forEach(trackNodeLocation -> {
                LOGGER.warn("trackNodeLocation.getLocation() " + trackNodeLocation.getLocation());
            });
            LOGGER.warn("Locate Node");
            final TrackNode node = railGraph.locateNode(targetLoc);
            LOGGER.warn("Locate Node null?" + (node == null) + " node?" + node);
        }
    }
}
