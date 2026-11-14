package org.valkyrienskies.clockwork.api.thinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record ThinkableContext(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
    public ThinkableContext {
        pos = pos.immutable();
    }
}
