package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.util.blocktype.IHeatableBlock;

public class AfterblazerBlock extends DirectionalBlock implements IHeatableBlock, IBE<AfterblazerBlockEntity> {
    public AfterblazerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AfterblazerBlockEntity> getBlockEntityClass() {
        return AfterblazerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AfterblazerBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.AFTERBLAZER.get();
    }
}
