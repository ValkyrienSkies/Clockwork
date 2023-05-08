package org.valkyrienskies.clockwork.content.contraptions.universal_joint;

import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class UniversalJointBlock extends DirectionalKineticBlock {
    public UniversalJointBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return null;
    }
}
