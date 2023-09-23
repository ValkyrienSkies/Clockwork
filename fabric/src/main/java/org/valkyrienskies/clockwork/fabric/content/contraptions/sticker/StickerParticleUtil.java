package org.valkyrienskies.clockwork.fabric.content.contraptions.sticker;

import com.simibubi.create.AllSoundEvents;
import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.util.compat.StickerParticleUtilInterface;

import java.util.function.Supplier;

public class StickerParticleUtil implements StickerParticleUtilInterface {

    @Environment(EnvType.CLIENT)
    public void playSound(Level level, boolean attach, BlockPos worldPosition, Direction facing) {
        AllSoundEvents.SLIME_ADDED.play(level, Minecraft.getInstance().player, worldPosition, 0.35f, attach ? 0.75f : 0.2f);
    }

    @Override
    public void doBluperParticle(Level level, BlockPos worldPosition, Direction facing) {
        if (level == null || facing == null)
            return;
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.playSound(level, true, worldPosition, facing));
    }

    @Override
    public void runOnClient(Supplier<Runnable> func){
        EnvExecutor.runWhenOn(EnvType.CLIENT, func);
    }
}
