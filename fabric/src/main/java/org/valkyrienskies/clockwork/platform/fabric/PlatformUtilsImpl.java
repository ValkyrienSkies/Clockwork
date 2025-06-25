package org.valkyrienskies.clockwork.platform.fabric;


import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.fabric.ClockworkModFabric;
import org.valkyrienskies.clockwork.fabric.FabricClockworkFluids;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;


import java.util.function.Supplier;

import static org.valkyrienskies.clockwork.ClockworkMod.MOD_ID;

public class PlatformUtilsImpl {

    public static void getEnvExecutor(Supplier<Runnable> toRun){
        EnvExecutor.runWhenOn(EnvType.CLIENT, toRun);
    }

    public static double getReachDistance(Player player) {
        return ReachEntityAttributes.getReachDistance(player, player.isCreative() ? 5.0 : 4.5);
    }


    public static void drainTank(SmartFluidTankBehaviour tank, int amount) {
        tank.getPrimaryHandler().getFluid().shrink(amount);
    }


    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }



    public static CompoundTag getExtraData(SmartBlockEntity be) {
        return be.getCustomData();
    }

    public static CreativeModeTab getCreativeTab() {
        return FabricItemGroup.builder().title(Component.translatableWithFallback("vs_clockwork:creative_mode_tab", "VS: Clockwork")).build();
    }
}