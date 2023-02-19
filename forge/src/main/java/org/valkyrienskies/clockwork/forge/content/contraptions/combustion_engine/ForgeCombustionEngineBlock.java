package org.valkyrienskies.clockwork.forge.content.contraptions.combustion_engine;

import com.simibubi.create.foundation.block.ITE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlock;
import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlockEntity;
import org.valkyrienskies.clockwork.forge.ForgeClockworkBlockEntities;

import java.util.Properties;

public class ForgeCombustionEngineBlock extends CombustionEngineBlock implements ITE<ForgeCombustionEngineBlockEntity> {

    public ForgeCombustionEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ForgeCombustionEngineBlockEntity> getTileEntityClass() {
        return ForgeCombustionEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ForgeCombustionEngineBlockEntity> getTileEntityType() {
        return ForgeClockworkBlockEntities.COMBUSTION_ENGINE.get();
    }
}

