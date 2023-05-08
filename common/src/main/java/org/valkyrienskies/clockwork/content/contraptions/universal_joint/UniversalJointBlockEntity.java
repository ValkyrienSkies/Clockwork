package org.valkyrienskies.clockwork.content.contraptions.universal_joint;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class UniversalJointBlockEntity extends KineticTileEntity {
    public UniversalJointBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void setConnectedPos(BlockPos pos) {
        //todo
    }
}
