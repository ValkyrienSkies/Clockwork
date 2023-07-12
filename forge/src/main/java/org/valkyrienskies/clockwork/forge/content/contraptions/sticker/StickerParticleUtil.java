package org.valkyrienskies.clockwork.forge.content.contraptions.sticker;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.valkyrienskies.clockwork.util.compat.StickerParticleUtilInterface;

import java.util.function.Supplier;

public class StickerParticleUtil implements StickerParticleUtilInterface {

    @OnlyIn(Dist.CLIENT)
    public void playSound(Level level, boolean attach, BlockPos worldPosition) {
        AllSoundEvents.SLIME_ADDED.play(level, Minecraft.getInstance().player, worldPosition, 0.35f, attach ? 0.75f : 0.2f);
    }

    @Override
    public void doBluperParticle(Level level, BlockPos worldPosition, Direction facing) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.playSound(level, true, worldPosition));
        }
    }

    @Override
    public void runOnClient(Supplier<Runnable> func){
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, func);
    }
}
