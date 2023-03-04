package org.valkyrienskies.clockwork.platform.fabric;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.entity.ExtraSpawnDataEntity;
import io.github.fabricators_of_create.porting_lib.mixin.common.accessor.ServerGamePacketListenerImplAccessor;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity.FuelType;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.InputKey;
import org.valkyrienskies.clockwork.fabric.FabricClockworkItems;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;
import org.valkyrienskies.clockwork.fabric.integration.cc_restiched.CCEvents;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;

import java.util.Set;

public class PlatformUtilsImpl {
    public static double getReachDistance(Player player) {
        return ReachEntityAttributes.getReachDistance(player, player.isCreative() ? 5.0 : 4.5);
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return ExtraSpawnDataEntity.createExtraDataSpawnPacket(entity);
    }

    public static void setAboveGroundTicks(ServerPlayer player, int ticks) {
        ((ServerGamePacketListenerImplAccessor) player.connection).port_lib$setAboveGroundTickCount(0);
    }

    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos,
                                                               ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {
        if (!state.hasBlockEntity())
            return InteractionResultHolder.fail(ItemStack.EMPTY);

        BlockEntity te = world.getBlockEntity(pos);
        if (!(te instanceof BalloonerBlockEntity))
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        BalloonerBlockEntity burnerTE = (BalloonerBlockEntity) te;

        if (burnerTE.isCreativeFuel(stack)) {
            if (!simulate)
                burnerTE.applyCreativeFuel();
            return InteractionResultHolder.success(ItemStack.EMPTY);
        }
        if (!burnerTE.tryUpdateFuel(stack, forceOverflow, simulate))
            return InteractionResultHolder.fail(ItemStack.EMPTY);

        if (!doNotConsume) {
            ItemStack container = stack.getRecipeRemainder();
            if (!world.isClientSide) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(container);
        }
        return InteractionResultHolder.success(ItemStack.EMPTY);
    }

    public static boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate, BalloonerBlockEntity blockEntity) {
        if (blockEntity.isCreative())
            return false;

        FuelType newFuel = FuelType.NONE;
        int newBurnTime;

        if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_SPECIAL.matches(itemStack)) {
            newBurnTime = 1000;
            newFuel = FuelType.SPECIAL;
        } else {
            Integer fuel = FuelRegistry.INSTANCE.get(itemStack.getItem());
            newBurnTime = (int) Math.min(fuel == null ? 0 : fuel, blockEntity.MAX_HEAT_CAPACITY * 0.95f);
            if (newBurnTime > 0)
                newFuel = FuelType.NORMAL;
            else if (AllTags.AllItemTags.BLAZE_BURNER_FUEL_REGULAR.matches(itemStack)) {
                newBurnTime = 1600; // Same as coal
                newFuel = FuelType.NORMAL;
            }
        }

        if (newFuel == FuelType.NONE)
            return false;
        if (newFuel.ordinal() < blockEntity.getActiveFuel().ordinal())
            return false;
        if (blockEntity.getActiveFuel() == FuelType.SPECIAL && blockEntity.getRemainingBurnTime() > 20)
            return false;

        if (newFuel == blockEntity.getActiveFuel()) {
            if (blockEntity.getRemainingBurnTime() + newBurnTime > blockEntity.MAX_HEAT_CAPACITY && !forceOverflow)
                return false;
            newBurnTime = Mth.clamp(blockEntity.getRemainingBurnTime() + newBurnTime, 0, blockEntity.MAX_HEAT_CAPACITY);
        }

        FuelType finalNewFuel = newFuel;
        int finalNewBurnTime = newBurnTime;
            blockEntity.activeFuel = finalNewFuel;
            blockEntity.remainingBurnTime = finalNewBurnTime;
            if (blockEntity.getLevel().isClientSide) {
                blockEntity.spawnParticleBurst(blockEntity.activeFuel == FuelType.SPECIAL, blockEntity.activeFuel == FuelType.HYPER);
                return true;
            }
            EngineHeatLevel prev = blockEntity.getHeatLevelFromBlock();
            blockEntity.playSound();
            blockEntity.updateBlockState();

            if (prev != blockEntity.getHeatLevelFromBlock())
                blockEntity.getLevel().playSound(null, blockEntity.getWorldPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS,
                        .125f + blockEntity.getLevel().random.nextFloat() * .125f, 1.15f - blockEntity.getLevel().random.nextFloat() * .25f);

        return true;
    }

    public static int maxBalloonRange() {
        return AllClockworkConfigs.SERVER.kinetics.balloonRange.get();
    }

    public static boolean isCannon(ItemStack stack) {
        return FabricClockworkItems.PASTRYMAKER.get().isCannon(stack);
    }

    public static void drainTank(SmartFluidTankBehaviour tank, int amount) {
        tank.getPrimaryHandler().getFluid().shrink(amount);
    }

    public static CWFluidTankBehaviour cwFluidTank(BehaviourType<CWFluidTankBehaviour> type, SmartTileEntity te, int tanks, long tankCapacity, boolean enforceVariety) {
        return new FabricCWFluidTankBehaviour(type, te, tanks, tankCapacity, enforceVariety);
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static void sequencedSeatKeysUpdated(ServerLevel level, BlockPos pos, Set<InputKey> keys) {
        CCEvents.sequencedSeatKeysUpdated(pos, keys);
    }
}
