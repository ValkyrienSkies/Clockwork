package org.valkyrienskies.clockwork.content.contraptions.combustion_engine;

import java.util.List;

import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CombustionEngineBlockEntity extends GeneratingKineticTileEntity {

    LerpedFloat arrowDirection;
    Couple<MutableBoolean> sidesToUpdate;
    boolean pressureUpdate;
    boolean reversed;

    public CombustionEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arrowDirection = LerpedFloat.linear()
                .startWithValue(1);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new FluidTankCTBehaviour(this));
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

    class PumpFluidTransferBehaviour extends FluidTankCTBehaviour {
        public PumpFluidTransferBehaviour(CTSpriteShiftEntry layerShift, CTSpriteShiftEntry topShift, CTSpriteShiftEntry innerShift) {
            super(layerShift, topShift, innerShift);
        }
    }
}