package org.valkyrienskies.clockwork.content.contraptions.combustion_engine;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.util.blocktype.*;

public abstract class CombustionEngineBlockEntity extends GeneratingKineticTileEntity implements IFuelableTileEntity {


    LerpedFloat arrowDirection;
    Couple<MutableBoolean> sidesToUpdate;
    boolean pressureUpdate;
    boolean reversed;

    float generatedSpeed;

    int remainingFuel;

    float stressCapacity;

    public CombustionEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arrowDirection = LerpedFloat.linear()
                .startWithValue(1);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(CombustionEngineBlock.FACING));
    }

    @Override
    public void removeSource() {
        if (hasSource() && isSource())
            reActivateSource = true;
        super.removeSource();
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

    @Override
    public LiquidFuelType getFuelQuality() {
        return null;
    }

    @Override
    public int getRemainingFuel() {
        return remainingFuel;
    }

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


}