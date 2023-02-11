package org.valkyrienskies.clockwork.mixin.compat.block;

import com.simibubi.create.content.contraptions.components.structureMovement.chassis.StickerBlock;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StickerBlock.class)
public abstract class MixinStickerBlock extends WrenchableDirectionalBlock {

    public MixinStickerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        ITE.onRemove(state, world, pos, newState);
    }
}
