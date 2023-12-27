package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
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
        return player.getEntityReach();
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return NetworkHooks.getEntitySpawningPacket(entity);
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static LiquidFuelType getLiquidFuelTypeFromItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return LiquidFuelType.NONE;
        }
        if (stack.getItem() instanceof BucketItem) {
            if (stack.is(ForgeClockworkFluids.VANILLA_FROSTING.get().getBucket())) {
                return LiquidFuelType.STALE;
            }
            if (stack.is(ForgeClockworkFluids.CHOCOLATE_FROSTING.get().getBucket())) {
                return LiquidFuelType.PLAIN;
            }
            if (stack.is(ForgeClockworkFluids.STRAWBERRY_FROSTING.get().getBucket())) {
                return LiquidFuelType.SWEET;
            }
        }
        return LiquidFuelType.NONE;
    }
}
