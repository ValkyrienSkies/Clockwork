package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class AfterblazerBlock extends DirectionalBlock implements IBE<AfterblazerEngineBlockEntity> {
    public AfterblazerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<AfterblazerEngineBlockEntity> getBlockEntityClass() {
        return AfterblazerEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AfterblazerEngineBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.AFTERBLAZER.get();
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                  boolean isMoving) {
        if (worldIn.isClientSide) {
            return;
        }
        withBlockEntityDo(worldIn, pos, te -> {
            te.getPower(worldIn, pos);
            te.setChanged();
        });
    }
}
