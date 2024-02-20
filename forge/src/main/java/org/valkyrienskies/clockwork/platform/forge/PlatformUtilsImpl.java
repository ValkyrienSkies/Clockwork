package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import org.valkyrienskies.clockwork.forge.ForgeClockworkFluids;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

public class PlatformUtilsImpl {
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
}
