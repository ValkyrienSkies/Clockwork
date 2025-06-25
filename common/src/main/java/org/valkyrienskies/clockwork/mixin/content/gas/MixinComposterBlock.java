package org.valkyrienskies.clockwork.mixin.content.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock;
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct;
import org.valkyrienskies.clockwork.util.Vector3dUtilsKt;
import org.valkyrienskies.kelvin.KelvinMod;
import org.valkyrienskies.kelvin.api.*;
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode;
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode;
import org.valkyrienskies.kelvin.impl.DuctNetworkServer;
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry;
import org.valkyrienskies.kelvin.util.KelvinExtensions;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashSet;
import java.util.Random;

@Mixin(ComposterBlock.class)
public class MixinComposterBlock extends Block implements INodeBlock {

    @Unique
    Double vs_clockwork$$maxPressure = 100000.0;


    public MixinComposterBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void vs_clockwork$$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (state.getValue(ComposterBlock.LEVEL) == 7) {
            DuctNetwork kelvin = ClockworkMod.getKelvin();
            ResourceLocation location =  VSGameUtilsKt.getResourceKey(VSGameUtilsKt.getDimensionId(level)).location();
            DuctNodePos ductNodePos = new DuctNodePos(pos.getX(), pos.getY(), pos.getZ(), location);

            double pressure = kelvin.getPressureAt(ductNodePos);

            GasType gas = GasTypeRegistry.INSTANCE.getGasType(KelvinMod.MOD_ID,"methane");

            if (pressure <= vs_clockwork$$maxPressure && gas != null) {
                kelvin.modGasMassOfTemperature(ductNodePos, gas, 10, 305);
            }
        }
    }


    @NotNull
    @Override
    public DuctNode createNode(@NotNull DuctNodePos pos) {
        return new PipeDuctNode(pos, NodeBehaviorType.PIPE, new HashSet<>(),0.05, 16375049.0, 1478.0);
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
        }
    }

    @Override
    public void nodeRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide()) {
            if (newState.isAir() || !(newState.getBlock() instanceof INodeBlock)) {
                ClockworkMod.getKelvin().removeNode(KelvinExtensions.INSTANCE.toDuctNodePos(pos, level.dimension().location()));
            }
        }
    }

}
