package org.valkyrienskies.clockwork.util.blocktype;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public interface IHeatableBlock {

    public float heat = 0.0f;

    public static final EnumProperty<EngineHeatLevel> HEAT_LEVEL = EnumProperty.create("heat", EngineHeatLevel.class);

    static EngineHeatLevel getHeatLevelOf(BlockState blockState) {
        return blockState.hasProperty(HEAT_LEVEL) ? blockState.getValue(HEAT_LEVEL)
                : EngineHeatLevel.SMOULDERING;
    }



}
