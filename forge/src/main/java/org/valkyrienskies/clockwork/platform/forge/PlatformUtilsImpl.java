package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkParticles;
import org.valkyrienskies.clockwork.forge.util.DuctStatsForge;
import org.valkyrienskies.clockwork.util.gui.DuctStats;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class PlatformUtilsImpl {

    public static void getEnvExecutor(Supplier<Runnable> toRun) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, toRun);
    }

    public static <D extends ParticleOptions> void registerParticleOnPlatform(ClockworkParticles.ParticleEntry<D> particleEntry, @Nullable Object event) {
        if (event instanceof RegisterParticleProvidersEvent pEvent) {
            particleEntry.typeFactory.get().register(particleEntry.object, pEvent);
        }

    }

    public static double getReachDistance(Player player) {
        return player.getEntityReach();
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return NetworkHooks.getEntitySpawningPacket(entity);
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }



    public static CompoundTag getExtraData(SmartBlockEntity be) {
        return be.getPersistentData();
    }

    public static CreativeModeTab getCreativeTab() {
        return CreativeModeTab.builder().title(Component.translatable("vs_clockwork:creative_mode_tab", "VS: Clockwork"))
            .icon(() -> new ItemStack(ClockworkBlocks.PHYSICS_INFUSER.get()))
            .build();
    }

    public static DuctStats getDuctStats(Block block) {
        return new DuctStatsForge(block);
    }
}
