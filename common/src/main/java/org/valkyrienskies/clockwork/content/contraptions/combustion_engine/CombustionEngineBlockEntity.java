package org.valkyrienskies.clockwork.content.contraptions.combustion_engine;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
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

    boolean first = true;

    public boolean active = false;

    public CWFluidTankBehaviour tank;

    int remainingFuel;

    float stressCapacity;

    boolean kineticsUpdated = false;

    LerpedFloat visualSpeed = LerpedFloat.linear();
    float angle;

    public CombustionEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arrowDirection = LerpedFloat.linear()
                .startWithValue(1);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(new TextComponent(spacing).append(new TextComponent("Draining").withStyle(ChatFormatting.GRAY)));
        tooltip.add(new TextComponent(spacing).append(new TextComponent(" " + getDrainRate() + "mb/t ")
                .withStyle(ChatFormatting.AQUA)).append(new TextComponent("with current fuel").withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    public float getFillState() {
        return (float) tank.getPrimaryHandler().getCurrentAmount() / tank.getPrimaryHandler().getTotalCapacity();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);

        compound.putBoolean("active", active);

        compound.putFloat("LastCapacityProvided", lastCapacityProvided);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        active = compound.getBoolean("active");
//        lastKnownPos = null;
//
//        if (compound.contains("LastKnownPos"))
//            lastKnownPos = NbtUtils.readBlockPos(compound.getCompound("LastKnownPos"));

        lastCapacityProvided = 0;

        if (compound.contains("LastCapacityProvided"))
            lastCapacityProvided = compound.getFloat("LastCapacityProvided");

        // Configure the chasing algorithm for visualSpeed
        if (clientPacket)
            visualSpeed.chase(getGeneratedSpeed(), 1 / 64f, Chaser.EXP);
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
        if (!hasSource() | getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            // Copied from FlywheelTileEntity::tick()
            float targetSpeed = getSpeed();
            visualSpeed.updateChaseTarget(targetSpeed);
            visualSpeed.tickChaser();
            angle += visualSpeed.getValue() * 3 / 10f;
            angle %= 360;
        }

        float speed = getSpeed();
        if (first) {

            updateGeneratedRotation();

            first = false;
        }
        float prevGeneratedSpeed = generatedSpeed;
        generatedSpeed = switch (getFuelQuality()) { //TODO move this inside fuel quality enum
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
            if (getRemainingFuel() >= getDrainRate()) {
                tank.getPrimaryHandler().shrink(getDrainRate());
            } else {
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

        if ((getRemainingFuel() < getDrainRate()) && active && !kineticsUpdated) {
            this.detachKinetics();
            kineticsUpdated = true;
        } else if (kineticsUpdated) {
            this.attachKinetics();
            kineticsUpdated = false;
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

    @Override
    protected Block getStressConfigKey() {
        return AllBlocks.WATER_WHEEL.get();
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
        return LiquidFuelType.fromFluid(fuel);
    }

    @Override
    public float calculateAddedStressCapacity() {
        capacity = Mth.abs(getGeneratedSpeed()) * 64;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!ClockWorkBlocks.COMBUSTION_ENGINE.has(getBlockState()))
            return 0;

        return convertToDirection(active ? generatedSpeed : 0, getBlockState().getValue(CombustionEngineBlock.FACING));
    }

    public boolean hasValidFuelType() {
        if (tank.isEmpty())
            return false;

        Fluid fuel = tank.getPrimaryHandler().getFluidType();
        return ClockWorkTags.AllFluidTags.isValidFuel(fuel);
    }

    @Override
    public int getRemainingFuel() {
        if (!hasValidFuelType()) {
            return 0;
        }

        return (int) tank.getPrimaryHandler().getCurrentAmount();
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

    @Override
    public CWFluidTankBehaviour getFluidTankBehaviour() {
        return tank;
    }

}