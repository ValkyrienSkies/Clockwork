package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.forge.ForgeClockworkFluids;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import java.util.function.Supplier;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

public class PlatformUtilsImpl {

    public static void getEnvExecutor(Supplier<Runnable> toRun) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, toRun);
    }

    public static double getReachDistance(Player player) {
        return player.getReachDistance();
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return NetworkHooks.getEntitySpawningPacket(entity);
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }



    public static CompoundTag getExtraData(SmartBlockEntity be) {
        return be.getTileData();
    }

    public static CreativeModeTab getCreativeTab() {
        return new CreativeModeTab("vs_clockwork") {
            @Override
            public ItemStack makeIcon() {
                return new ItemStack(ClockworkItems.GRAVITRON.get());
            }
        };
    }
}
