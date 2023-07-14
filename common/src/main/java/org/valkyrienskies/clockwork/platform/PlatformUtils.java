package org.valkyrienskies.clockwork.platform;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.InputKey;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

import java.util.Set;

public class PlatformUtils {

    @ExpectPlatform
    public static double getReachDistance(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setAboveGroundTicks(ServerPlayer player, int ticks) {
        throw new AssertionError();
    }

//    @ExpectPlatform
//    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos,
//                                                               ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {throw new AssertionError();}

//    @ExpectPlatform
//    public static boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate, BalloonerBlockEntity blockEntity) {throw new AssertionError();}
    @ExpectPlatform
    public static int maxBalloonRange() {throw new AssertionError();}

    @ExpectPlatform
    public static void useBallooner(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult blockRayTraceResult) {throw new AssertionError();}

//    @ExpectPlatform
//    public static Class<?> getCombustionEngineTileEntityClass() {throw new AssertionError();}
//
//    @ExpectPlatform
//    public static BlockEntityType<? extends CombustionEngineBlockEntity> getCombustionEngineTileEntityType() {throw new AssertionError();}

    @ExpectPlatform
    public static boolean isCannon(ItemStack stack) {throw new AssertionError();}

    @ExpectPlatform
    public static CWFluidTankBehaviour cwFluidTank(BehaviourType<CWFluidTankBehaviour> type, SmartBlockEntity te, int tanks, long tankCapacity, boolean enforceVariety) {throw new AssertionError();}

    @ExpectPlatform
    public static boolean isModLoaded(String modId) {throw new AssertionError();}

    @ExpectPlatform
    public static void sequencedSeatKeysUpdated(ServerLevel level, BlockPos pos, Set<InputKey> keys) {throw new AssertionError();}

    @ExpectPlatform
    public static LiquidFuelType getLiquidFuelTypeFromItemStack(ItemStack stack) {throw new AssertionError();}
}
