package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.util.blocktype.IFuelableBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

import java.util.List;

public class AfterblazerBlockEntity extends SmartBlockEntity implements IFuelableBlockEntity {

    public CWFluidTankBehaviour tank;


    public AfterblazerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = CWFluidTankBehaviour.single(this, 8000);
        behaviours.add(tank);
    }

    @Override
    public LiquidFuelType getFuelQuality() {
        return null;
    }

    @Override
    public int getRemainingFuel() {
        return 0;
    }

    @Override
    public int getDrainRate() {
        return 0;
    }
}
