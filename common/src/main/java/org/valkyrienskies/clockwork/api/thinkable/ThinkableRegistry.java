package org.valkyrienskies.clockwork.api.thinkable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ThinkableRegistry {
    /** Register during your compatibility entrypoint */
    void registerBlock(Block block, Function<ThinkableContext, Thinkable> provider);
    <T extends BlockEntity> void registerBlockEntity(BlockEntityType<T> type, BiFunction<ThinkableContext, T, Thinkable> provider);
}
