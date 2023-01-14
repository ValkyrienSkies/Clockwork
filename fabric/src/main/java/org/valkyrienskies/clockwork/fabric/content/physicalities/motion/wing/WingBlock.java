package org.valkyrienskies.clockwork.fabric.content.physicalities.motion.wing;

import com.simibubi.create.content.contraptions.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.valkyrienskies.clockwork.fabric.util.blocktype.TriAxisBlock;

public class WingBlock extends TriAxisBlock {

    public static final BooleanProperty connectedOne = BooleanProperty.create("connected_one");
    public static final BooleanProperty connectedTwo = BooleanProperty.create("connected_two");
    public static final BooleanProperty connectedThree = BooleanProperty.create("connected_three");
    public static final BooleanProperty connectedFour = BooleanProperty.create("connected_four");


    public WingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);


    }
}
