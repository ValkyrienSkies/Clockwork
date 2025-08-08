package org.valkyrienskies.clockwork.mixin.content.gas_engine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlockEntity;

import java.lang.ref.WeakReference;

@Mixin(SteamEngineBlockEntity.class)
public abstract class MixinSteamEngineBlockEntity extends SmartBlockEntity {

    @Unique
    public WeakReference<GasEngineBlockEntity> gasSource = new WeakReference<>(null);

    @Unique
    private boolean generating = false;

    @Shadow
    protected ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    public MixinSteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }


    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void vs_clockwork$tick(CallbackInfo ci) {
        FluidTankBlockEntity tank = getTank();
        if (tank != null)  return;

        PoweredShaftBlockEntity shaft = getShaft();
        GasEngineBlockEntity engine = getEngine();

        if (engine == null || shaft == null) {
            if (level.isClientSide())
                return;
            if (shaft == null)
                return;
            if (!shaft.getBlockPos()
                    .subtract(worldPosition)
                    .equals(shaft.enginePos))
                return;
            if (shaft.engineEfficiency == 0)
                return;
            Direction facing = SteamEngineBlock.getFacing(getBlockState());
            if (level.isLoaded(worldPosition.relative(facing.getOpposite())))
                shaft.update(worldPosition, 0, 0);
            return;
        }

        BlockState shaftState = shaft.getBlockState();
        Direction.Axis targetAxis = Direction.Axis.X;
        if (shaftState.getBlock()instanceof IRotate ir)
            targetAxis = ir.getRotationAxis(shaftState);
        boolean verticalTarget = targetAxis == Direction.Axis.Y;

        BlockState blockState = getBlockState();
        if (!AllBlocks.STEAM_ENGINE.has(blockState)) return;

        Direction facing = SteamEngineBlock.getFacing(blockState);
        if (facing.getAxis() == Direction.Axis.Y)
            facing = blockState.getValue(SteamEngineBlock.FACING);

        float efficiency = engine.getEngineEfficiency();
        if (efficiency > 0) award(AllAdvancements.STEAM_ENGINE);


        int conveyedSpeedLevel =
                efficiency == 0 ? 1 : verticalTarget ? 1 : (int) GeneratingKineticBlockEntity.convertToDirection(1, facing);
        if (targetAxis == Direction.Axis.Z)
            conveyedSpeedLevel *= -1;
        if (movementDirection.get() == WindmillBearingBlockEntity.RotationDirection.COUNTER_CLOCKWISE)
            conveyedSpeedLevel *= -1;

        float shaftSpeed = shaft.getTheoreticalSpeed();
        if (shaft.hasSource() && shaftSpeed != 0 && conveyedSpeedLevel != 0
                && (shaftSpeed > 0) != (conveyedSpeedLevel > 0)) {
            movementDirection.setValue(1 - movementDirection.get()
                    .ordinal());
            conveyedSpeedLevel *= -1;
        }

        shaft.update(worldPosition, conveyedSpeedLevel, efficiency);

        if (!level.isClientSide) return;

        if (efficiency > 0) engine.spawnParticles( (ClientLevel) level,
                new Vector3d(getBlockPos().getX() + level.random.nextDouble(), getBlockPos().getY() + level.random.nextDouble(), getBlockPos().getZ() + level.random.nextDouble()),
                new Vector3d(0.0,0.0,0.0));


    }

    // Prevent shaft from updating (due to no tank) if there's a gas engine
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z"), cancellable = true)
    private void cancelShaftUpdate(CallbackInfo ci) {
        if (getEngine() != null) ci.cancel();
    }


    @Shadow public abstract PoweredShaftBlockEntity getShaft();

    @Shadow public abstract FluidTankBlockEntity getTank();


    @Unique
    public GasEngineBlockEntity getEngine() {
        GasEngineBlockEntity engine = gasSource.get();
        if (engine == null || engine.isRemoved()) {
            if (engine != null)
                gasSource = new WeakReference<>(null);
            Direction facing = SteamEngineBlock.getFacing(getBlockState());
            BlockEntity be = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
            if (be instanceof GasEngineBlockEntity engineBe)
                gasSource = new WeakReference<>(engineBe);
        }
        return engine;
    }
}
