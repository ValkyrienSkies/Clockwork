package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.VecHelper;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlock;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity.FuelType;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlock.EngineHeatLevel;
import org.valkyrienskies.clockwork.platform.PlatformUtils;

import java.util.Random;

public class BalloonerBlockEntity extends KineticTileEntity implements IHaveGoggleInformation, IHotAirProducer {

    public static final int MAX_HEAT_CAPACITY = 10000;
    public static final int INSERTION_THRESHOLD = 500;

    public FuelType activeFuel;
    public int remainingBurnTime;

    protected boolean isCreative;
    protected boolean pissedOff;

    public BalloonerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        activeFuel = FuelType.NONE;
        remainingBurnTime = 0;

        isCreative = false;


    }

    public BlockPos getWorldPosition() {
        return this.worldPosition;
    }

    public FuelType getActiveFuel() {
        return activeFuel;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    public boolean isCreative() {
        return isCreative;
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
//            tickAnimation();
            if (!isVirtual())
                spawnParticles(getHeatLevelFromBlock(), 1);
            return;
        }

        if (pissedOff) {
            applyHyperFuel();
            pissedOff = false;
        }

        if (isCreative)
            return;

        if (remainingBurnTime > 0)
            remainingBurnTime--;

        if (activeFuel == FuelType.NORMAL)
            updateBlockState();
        if (remainingBurnTime > 0)
            return;

        if (activeFuel == FuelType.SPECIAL) {
            activeFuel = FuelType.NORMAL;
            remainingBurnTime = MAX_HEAT_CAPACITY / 2;
        } else
            activeFuel = FuelType.NONE;

        updateBlockState();
    }

    public boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        return PlatformUtils.tryUpdateFuel(itemStack, forceOverflow, simulate, this);
    }


    public void updateBlockState() {
        setBlockHeat(getHeatLevelFromFuelType(activeFuel));
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        if (!isCreative) {
            compound.putInt("fuelLevel", activeFuel.ordinal());
            compound.putInt("burnTimeRemaining", remainingBurnTime);
        } else
            compound.putBoolean("isCreative", true);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        activeFuel = FuelType.values()[compound.getInt("fuelLevel")];
        remainingBurnTime = compound.getInt("burnTimeRemaining");
        isCreative = compound.getBoolean("isCreative");
        super.read(compound, clientPacket);
    }


    protected void applyHyperFuel() {
        remainingBurnTime = remainingBurnTime = MAX_HEAT_CAPACITY / 2;

        EngineHeatLevel next = EngineHeatLevel.INFURIATED;

        if (level.isClientSide) {
            spawnParticleBurst(next.isAtLeast(EngineHeatLevel.SEETHING), next.isAtLeast(EngineHeatLevel.INFURIATED));
            return;
        }

        playSound();
        setBlockHeat(next);
    }

    protected void spawnParticles(EngineHeatLevel heatLevel, double burstMult) {
        if (level == null)
            return;

        Random r = level.getRandom();

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f)
                .multiply(1, 0, 1));

        if (r.nextInt(4) != 0)
            return;

        boolean empty = level.getBlockState(worldPosition.above())
                .getCollisionShape(level, worldPosition.above())
                .isEmpty();

        if (empty || r.nextInt(8) == 0)
            level.addParticle(ParticleTypes.LARGE_SMOKE, v.x, v.y, v.z, 0, 0, 0);

        double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f)
                        .multiply(1, .25f, 1)
                        .normalize()
                        .scale((empty ? .25f : .5) + r.nextDouble() * .125f))
                .add(0, .5, 0);
        if (heatLevel.isAtLeast(EngineHeatLevel.INFURIATED)) {
            level.addParticle(ParticleTypes.PORTAL, v2.x, v2.y, v2.z, 0, yMotion, 0);
        } else if (heatLevel.isAtLeast(EngineHeatLevel.SEETHING)) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
        } else if (heatLevel.isAtLeast(EngineHeatLevel.FADING)) {
            level.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
        }
        return;
    }

    public void spawnParticleBurst(boolean soulFlame, boolean purpleFlame) {
        Vec3 c = VecHelper.getCenterOf(worldPosition);
        Random r = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, r, .5f)
                    .multiply(1, .25f, 1)
                    .normalize();
            Vec3 v = c.add(offset.scale(.5 + r.nextDouble() * .125f))
                    .add(0, .125, 0);
            Vec3 m = offset.scale(1 / 32f);
            if (purpleFlame) {
                level.addParticle(ParticleTypes.PORTAL, v.x, v.y, v.z, m.x, m.y, m.z);
            } else if
            (soulFlame) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v.x, v.y, v.z, m.x, m.y, m.z);
            } else {
                level.addParticle(ParticleTypes.FLAME, v.x, v.y, v.z, m.x, m.y, m.z);
            }
        }
    }

    public void applyCreativeFuel() {
        activeFuel = FuelType.NONE;
        remainingBurnTime = 0;
        isCreative = true;

        EngineHeatLevel next = getHeatLevelFromBlock().nextActiveLevel();

        if (level.isClientSide) {
            spawnParticleBurst(next.isAtLeast(EngineHeatLevel.SEETHING), next.isAtLeast(EngineHeatLevel.INFURIATED));
            return;
        }

        playSound();
        if (next == EngineHeatLevel.FADING)
            next = next.nextActiveLevel();
        setBlockHeat(next);
    }

    public void setBlockHeat(EngineHeatLevel heat) {
        EngineHeatLevel inBlockState = getHeatLevelFromBlock();
        if (inBlockState == heat)
            return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(AfterblazerBlock.HEAT_LEVEL, heat));
        notifyUpdate();
    }

    public boolean isCreativeFuel(ItemStack stack) {
        return AllItems.CREATIVE_BLAZE_CAKE.isIn(stack);
    }

    public boolean isHyperFuel(ItemStack stack) {
        return ClockWorkItems.STRATODONUT.isIn(stack);
    }

    public void playSound() {
        level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS,
                .125f + level.random.nextFloat() * .125f, .75f - level.random.nextFloat() * .25f);
    }

    public EngineHeatLevel getHeatLevelFromFuelType(FuelType fuel) {
        EngineHeatLevel level = EngineHeatLevel.SMOULDERING;
        switch (activeFuel) {
            case HYPER:
                level = EngineHeatLevel.INFURIATED;
                break;
            case SPECIAL:
                level = EngineHeatLevel.SEETHING;
                break;
            case NORMAL:
                boolean lowPercent = (double) remainingBurnTime / MAX_HEAT_CAPACITY < 0.0125;
                level = lowPercent ? EngineHeatLevel.FADING : EngineHeatLevel.KINDLED;
                break;
            default:
            case NONE:
                break;
        }
        return level;
    }

    public EngineHeatLevel getHeatLevelFromBlock() {
        return BalloonerBlock.getHeatLevelOf(getBlockState());
    }

    @Override
    public boolean makesHotAir() {
        return true;
    }

    @Override
    public double getHotAirProduction() {
        return 0;
    }
}
