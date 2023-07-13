package org.valkyrienskies.clockwork.mixin.content.balloon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonData;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerBlock;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.content.forces.BalloonController;
import org.valkyrienskies.clockwork.data.ClockWorkTags;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashSet;
import java.util.Set;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunkBalloon {
    @Shadow
    @Final
    Level level;

    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState", at = @At("TAIL"))
    public void postSetBlockState(final BlockPos pos, final BlockState state, final boolean moved,
                                  final CallbackInfoReturnable<BlockState> cir) {
        final BlockState prevState = cir.getReturnValue();
        LoadedServerShip ship = null;
        if (prevState == state) {
            return;
        }
        if (!level.isClientSide) {
            ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, pos);
        }
        Set<BalloonData> balloonControllers = new HashSet<>();
        if (ship != null) {
            if (ship.getAttachment(BalloonController.class) != null) {
                if (!BalloonController.getOrCreate(ship).balloonData.isEmpty()) {
                    balloonControllers.addAll(BalloonController.getOrCreate(ship).balloonData.values());
//                    for (int i = BalloonController.getOrCreate(ship).balloonData.size()-1; i > 0; i--) {
//                        balloonControllers.add(BalloonController.getOrCreate(ship).balloonData.get(i));
//                    }
                }
            }
            for (BalloonData data : balloonControllers) {
                BlockPos checkpos = new BlockPos(data.burnerPos.x(), data.burnerPos.y(), data.burnerPos.z());
                BlockState checkstate = level.getBlockState(checkpos);
                if (checkstate.getBlock() instanceof BalloonerBlock) {
                    BalloonerBlockEntity ballooner = (BalloonerBlockEntity) getBlockEntity(checkpos);
                    if (ballooner != null) {
                        if (!ballooner.getBalloonPositions().isEmpty()) {
                            if (ballooner.getBalloonPositions().contains(pos)) {
                                if (!(state.is(ClockWorkTags.AllBlockTags.BALLOON_BLOCK.tag))) {
                                    ballooner.tryCheck();
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
