package org.valkyrienskies.clockwork.content.contraptions.combustion_engine;

import java.util.List;

import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import net.minecraft.nbt.CompoundTag;
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
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.*;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

public class CombustionEngineBlockEntity extends GeneratingKineticTileEntity implements IFuelableTileEntity, IHaveGoggleInformation, SmartFluidTankBlockEntity {


    LerpedFloat arrowDirection;
    Couple<MutableBoolean> sidesToUpdate;
    boolean pressureUpdate;
    boolean reversed;

    float generatedSpeed;

    public boolean active = false;

    public CWFluidTankBehaviour tank;

    int remainingFuel;

    float stressCapacity;

    public CombustionEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arrowDirection = LerpedFloat.linear()
                .startWithValue(1);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }

    public float getFillState() {
        return (float) tank.getPrimaryHandler().getAmount() / tank.getPrimaryHandler().getTotalCapacity();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);

        compound.putFloat("LastCapacityProvided", lastCapacityProvided);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

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

//    @Override
//    public void setSource(BlockPos source) {
//        super.setSource(source);
//        BlockEntity tileEntity = level.getBlockEntity(source);
//        if (!(tileEntity instanceof KineticTileEntity))
//            return;
//        KineticTileEntity sourceTe = (KineticTileEntity) tileEntity;
//        if (reActivateSource && Math.abs(sourceTe.getSpeed()) >= Math.abs(getGeneratedSpeed()))
//            reActivateSource = false;
//    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        tank = CWFluidTankBehaviour.single(this, 8000);
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
                tank.getPrimaryHandler().shrink(getDrainRate());
            } else {
                tank.getPrimaryHandler().shrink(getRemainingFuel());
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
        Fluid fuel = tank.getPrimaryHandler().getFluidType();
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

//    @Override
//    public float calculateAddedStressCapacity() {
//        return Math.abs(getGeneratedSpeed()) * 64;
//    }

    @Override
    public float getGeneratedSpeed() {
        if (!ClockWorkBlocks.COMBUSTION_ENGINE.has(getBlockState()))
            return 0;

        return convertToDirection(generatedSpeed, getBlockState().getValue(CombustionEngineBlock.FACING));
    }

    public boolean hasValidFuelType() {
        if (tank.isEmpty())
            return false;

        Fluid fuel = tank.getPrimaryHandler().getFluidType();

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

        return (int) tank.getPrimaryHandler().getAmount();
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

    @Override
    public CWFluidTankBehaviour getFluidTankBehaviour() {
        return tank;
    }

}