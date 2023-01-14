package org.valkyrienskies.clockwork.fabric.util.blocktype;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class TriAxisBlock extends Block {
    public static final EnumProperty AXIS;

    protected TriAxisBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    static {
        AXIS = BlockStateProperties.AXIS;
    }
}
