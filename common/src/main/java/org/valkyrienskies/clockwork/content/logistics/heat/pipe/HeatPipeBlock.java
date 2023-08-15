package org.valkyrienskies.clockwork.content.logistics.heat.pipe;

import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class HeatPipeBlock extends PipeBlock implements SimpleWaterloggedBlock, IWrenchable, IBE<HeatPipeBlockEntity>, ITransformableBlock {
    public HeatPipeBlock(Properties properties) {
        super(6/16f, properties);
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        return null;
    }

    @Override
    public Class<HeatPipeBlockEntity> getBlockEntityClass() {
        return HeatPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeatPipeBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.HEAT_PIPE.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return IWrenchable.super.onWrenched(state, context);
    }
}
