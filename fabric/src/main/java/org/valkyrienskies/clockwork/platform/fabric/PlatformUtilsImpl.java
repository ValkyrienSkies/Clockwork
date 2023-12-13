package org.valkyrienskies.clockwork.platform.fabric;


import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.entity.ExtraSpawnDataEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.fabric.FabricClockworkFluids;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

import java.util.Set;

public class PlatformUtilsImpl {
    public static double getReachDistance(Player player) {
        return ReachEntityAttributes.getReachDistance(player, player.isCreative() ? 5.0 : 4.5);
    }


    public static void drainTank(SmartFluidTankBehaviour tank, int amount) {
        tank.getPrimaryHandler().getFluid().shrink(amount);
    }


    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static LiquidFuelType getLiquidFuelTypeFromItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return LiquidFuelType.NONE;
        }
        if (stack.getItem() instanceof BucketItem) {
            if (stack.is(FabricClockworkFluids.VANILLA_FROSTING.get().getBucket())) {
                return LiquidFuelType.STALE;
            }
            if (stack.is(FabricClockworkFluids.CHOCOLATE_FROSTING.get().getBucket())) {
                return LiquidFuelType.PLAIN;
            }
            if (stack.is(FabricClockworkFluids.STRAWBERRY_FROSTING.get().getBucket())) {
                return LiquidFuelType.SWEET;
            }
        }
        return LiquidFuelType.NONE;
    }
}