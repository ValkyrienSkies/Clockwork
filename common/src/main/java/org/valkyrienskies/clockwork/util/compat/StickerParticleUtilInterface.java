package org.valkyrienskies.clockwork.util.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public interface StickerParticleUtilInterface {

    public void doBluperParticle(Level level, BlockPos worldPosition, Direction facing);

    public void runOnClient(Supplier<Runnable> func);
}
