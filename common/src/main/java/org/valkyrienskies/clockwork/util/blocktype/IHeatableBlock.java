package org.valkyrienskies.clockwork.util.blocktype;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlock;

public interface IHeatableBlock {

    public static final EnumProperty<EngineHeatLevel> HEAT_LEVEL = EnumProperty.create("blaze", EngineHeatLevel.class);

    static EngineHeatLevel getHeatLevelOf(BlockState blockState) {
        return blockState.hasProperty(HEAT_LEVEL) ? blockState.getValue(HEAT_LEVEL)
                : EngineHeatLevel.SMOULDERING;
    }

    static int getLight(BlockState state) {
        EngineHeatLevel level = state.getValue(HEAT_LEVEL);
        return switch (level) {
            case SMOULDERING -> 8;
            case INFURIATED -> 20;
            default -> 15;
        };
    }

}
