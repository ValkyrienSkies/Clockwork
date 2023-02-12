package org.valkyrienskies.clockwork.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlockEntity;

import javax.annotation.Nullable;

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

    @ExpectPlatform
    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos,
                                                               ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {throw new AssertionError();}

    @ExpectPlatform
    public static boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate, BalloonerBlockEntity blockEntity) {throw new AssertionError();}
    @ExpectPlatform
    public static int maxBalloonRange() {throw new AssertionError();}

    @ExpectPlatform
    public void useBallooner(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult blockRayTraceResult) {throw new AssertionError();}
}
