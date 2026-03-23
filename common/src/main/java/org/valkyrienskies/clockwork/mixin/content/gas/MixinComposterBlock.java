package org.valkyrienskies.clockwork.mixin.content.gas;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkGasses;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats;
import org.valkyrienskies.clockwork.util.gui.ProductionInfo;
import org.valkyrienskies.clockwork.util.gui.ProductionMethod;
import org.valkyrienskies.clockwork.util.gui.ProductionType;
import org.valkyrienskies.kelvin.KelvinMod;
import org.valkyrienskies.kelvin.api.*;
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode;
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry;
import org.valkyrienskies.kelvin.util.INodeBlock;
import org.valkyrienskies.kelvin.util.KelvinExtensions;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mixin(ComposterBlock.class)
public class MixinComposterBlock extends Block implements INodeBlock, IHaveDuctStats {

    @Unique
    private static final double vs_clockwork$$maxPressure = 100000.0;


    public MixinComposterBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void vs_clockwork$$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (state.getValue(ComposterBlock.LEVEL) == 7) {
            DuctNetwork kelvin = ClockworkMod.getKelvin();
            ResourceLocation location =  VSGameUtilsKt.getResourceKey(VSGameUtilsKt.getDimensionId(level)).location();
            DuctNodePos ductNodePos = new DuctNodePos(pos.getX(), pos.getY(), pos.getZ(), location);

            double pressure = kelvin.getPressureAt(ductNodePos);

            if (pressure <= vs_clockwork$$maxPressure) {
                kelvin.addGasAtTemperature(ductNodePos, ClockworkGasses.INSTANCE.getMETHANE(), 0.05, 305);
            }
        }
    }


    @NotNull
    @Override
    public DuctNode createNode(@NotNull DuctNodePos pos) {
        return new PipeDuctNode(pos, NodeBehaviorType.PIPE, new HashSet<>(),1.0, 16375049.0, 1478.0, 1687.5, 44.9);
    }

    @Override
    public boolean canConnectTo(@NotNull BlockPos self, @NotNull BlockPos other, @NotNull Direction direction, @NotNull BlockGetter level) {
        return self.distSqr(other) <= 1.0 && direction != Direction.UP;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        nodeRemove(pState, pLevel, pPos, pState, pIsMoving);
        super.onRemove(pState, pLevel, pPos, pState, pIsMoving);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        nodePlace(pState,pLevel,pPos,pState,pIsMoving);
        super.onPlace(pState, pLevel, pPos, pState, pIsMoving);

    }

    @Override
    public void nodePlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            if (state.isAir() || !(state.getBlock() instanceof INodeBlock)) {
                return;
            }
            ClockworkMod.getKelvin().addNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location()), createNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location())));
        } else {
            //ClockworkModClient.getKelvin().addNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location()), createNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location())));
        }
    }

    @Override
    public void nodeRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide()) {
            if (newState.isAir() || !(newState.getBlock() instanceof INodeBlock)) {
                ClockworkMod.getKelvin().removeNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location()));
            }
        } else {
            ClockworkModClient.getKelvin().removeNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location()));
        }
    }

    @Override
    public void nodeAddClient(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos) {

    }

    @Override
    public void nodeRemoveClient(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos) {

    }

    @Override
    public @NotNull Map<@NotNull ResourceLocation, @NotNull ProductionInfo> getProductionStats() {
        HashMap<ResourceLocation, ProductionInfo> stats = new HashMap();
        ResourceLocation methaneLocation = ClockworkGasses.INSTANCE.getMETHANE().getResourceLocation();
        stats.put(methaneLocation, new ProductionInfo(
                ProductionMethod.OTHER,
                ProductionType.CONDITIONAL,
                Component.translatable("vs_clockwork.production_condition.composter_methane")
        ));
        return stats;
    }
}
