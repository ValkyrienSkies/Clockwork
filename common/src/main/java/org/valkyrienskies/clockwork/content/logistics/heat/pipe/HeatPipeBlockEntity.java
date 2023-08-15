package org.valkyrienskies.clockwork.content.logistics.heat.pipe;

import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatContainer;

import java.util.List;

public class HeatPipeBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity, IHeatContainer {
    public HeatPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void transform(StructureTransform transform) {

    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }
}
