package org.valkyrienskies.clockwork.content.contraptions.combustion_engine;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.injectables.annotations.PlatformOnly;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.data.ClockWorkTags;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.util.blocktype.*;

import javax.annotation.Nullable;

import static dev.architectury.injectables.annotations.PlatformOnly.FABRIC;
import static dev.architectury.injectables.annotations.PlatformOnly.FORGE;

public class CombustionEngineBlockEntity extends GeneratingKineticTileEntity implements IFuelableTileEntity, IHaveGoggleInformation {


    LerpedFloat arrowDirection;
    Couple<MutableBoolean> sidesToUpdate;
    boolean pressureUpdate;
    boolean reversed;

    float generatedSpeed;

    public boolean active = false;

    public SmartFluidTankBehaviour tank;

    int remainingFuel;

    float stressCapacity;

    public CombustionEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arrowDirection = LerpedFloat.linear()
                .startWithValue(1);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }
    @Override
    public void removeSource() {
        if (hasSource() && isSource())
            reActivateSource = true;
        super.removeSource();
    }

    public float getFillState() {
        return (float) tank.getPrimaryHandler().getFluidAmount() / tank.getPrimaryHandler().getCapacity();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("Tank", tank.getPrimaryHandler().writeToNBT(new CompoundTag()));
        compound.putFloat("LastCapacityProvided", lastCapacityProvided);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        tank.getPrimaryHandler().readFromNBT(compound.getCompound("Tank"));

//        lastKnownPos = null;
//
//        if (compound.contains("LastKnownPos"))
//            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));

        lastCapacityProvided = 0;

        if (compound.contains("LastCapacityProvided"))
            lastCapacityProvided = compound.getFloat("LastCapacityProvided");

        if (!clientPacket)
            return;


    }

    @Override
    public void setSource(BlockPos source) {
        super.setSource(source);
        BlockEntity tileEntity = level.getBlockEntity(source);
        if (!(tileEntity instanceof KineticTileEntity))
            return;
        KineticTileEntity sourceTe = (KineticTileEntity) tileEntity;
        if (reActivateSource && Math.abs(sourceTe.getSpeed()) >= Math.abs(getGeneratedSpeed()))
            reActivateSource = false;
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 8000);
        super.addBehaviours(behaviours);
    }

    @Override
    public void initialize() {
        super.initialize();
        reversed = getSpeed() < 0;
    }

    @Override
    public void tick() {
        super.tick();
        float speed = getSpeed();

        float prevGeneratedSpeed = generatedSpeed;
        generatedSpeed = switch (getFuelQuality()) {
            case NONE -> 0;
            case STALE -> 16;
            case PLAIN -> 32;
            case SWEET, EXTRA -> 64;
            case GOURMET -> 128;
        };
        if (prevGeneratedSpeed != generatedSpeed) {
            reActivateSource = true;
        }

        if (reActivateSource) {
            updateGeneratedRotation();
            reActivateSource = false;
        }

        if (hasValidFuelType()) {
            active = true;
            if (getRemainingFuel() >= 2) {
                tank.getPrimaryHandler().getFluid().shrink(getDrainRate());
//                tank.getPrimaryHandler().drain(getDrainRate(), IFluidHandler.FluidAction.EXECUTE);
            } else {
//                tank.getPrimaryHandler().drain(getRemainingFuel(), IFluidHandler.FluidAction.EXECUTE);
                tank.getPrimaryHandler().getFluid().shrink(getRemainingFuel());
                active = false;
            }

        }

        if (level.isClientSide) {
            if (speed == 0)
                return;
            arrowDirection.chase(speed >= 0 ? 1 : -1, .5f, Chaser.EXP);
            arrowDirection.tickChaser();
            if (!isVirtual())
                return;
        }

//		if (pressureUpdate)
//			updatePressureChange();

        sidesToUpdate.forEachWithContext((update, isFront) -> {
            if (update.isFalse())
                return;
            update.setFalse();
        });

        if (speed == 0)
            return;
        if (speed < 0 != reversed) {
            reversed = speed < 0;
            return;
        }
    }

//    void onFluidStackChanged()

//    public static void drainTank(SmartFluidTankBehaviour tank, int amount) {
//        PlatformUtils.drainTank(tank, amount);
//    }

    @Override
    public LiquidFuelType getFuelQuality() {
        if (!hasValidFuelType()) {
            return LiquidFuelType.NONE;
        }
        Fluid fuel = tank.getPrimaryHandler().getFluid().getFluid();
        if (fuel.is(ClockWorkTags.AllFluidTags.STALE.tag)) {
            return LiquidFuelType.STALE;
        } else if (fuel.is(ClockWorkTags.AllFluidTags.PLAIN.tag)) {
            return LiquidFuelType.PLAIN;
        } else if (fuel.is(ClockWorkTags.AllFluidTags.SWEET.tag)) {
            return LiquidFuelType.SWEET;
        } else if (fuel.is(ClockWorkTags.AllFluidTags.GOURMET.tag)) {
            return LiquidFuelType.GOURMET;
        } else {
            return LiquidFuelType.EXTRA;
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        return Math.abs(getGeneratedSpeed()) * 64;
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(CombustionEngineBlock.FACING));
    }

    public boolean hasValidFuelType() {
        if (tank.isEmpty())
            return false;

        Fluid fuel = tank.getPrimaryHandler().getFluid().getFluid();

        if (fuel.is(ClockWorkTags.AllFluidTags.STALE.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.PLAIN.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.SWEET.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.EXTRA.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.GOURMET.tag))
            return true;

        return false;
    }

    @Override
    public int getRemainingFuel() {
        if (!hasValidFuelType()) {
            return 0;
        }

        return (int) tank.getPrimaryHandler().getFluidAmount();
    }
    // TODO: PASTRIES
    @Override
    public int getDrainRate() {
        return 2;
    }

    @Override
    public FuelBoosterType getFuelBooster() {
        return null;
    }

    public EngineHeatLevel getHeatLevelFromBlock() {
        return IHeatableBlock.getHeatLevelOf(getBlockState());
    }

//    @PlatformOnly(FABRIC)
//    @Nullable
//    @Override
//    public Storage<FluidVariant> getFluidStorage(@Nullable Direction direction) {
//        return null;
//    }
//
//    @PlatformOnly(FORGE)
//    @Override
//    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
//        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && side != Direction.DOWN)
//            return tank.getCapability()
//                    .cast();
//        return super.getCapability(cap, side);
//    }


}