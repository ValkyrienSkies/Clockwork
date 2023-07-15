package org.valkyrienskies.clockwork.content.propulsion.ballooner;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.VecHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.BalloonController;
import org.valkyrienskies.clockwork.data.ClockWorkTags;
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.IFuelableBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BalloonerBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation, IHotAirProducer, IFuelableBlockEntity, SmartFluidTankBlockEntity {

    public static final int MAX_HEAT_CAPACITY = 10000;
    public static final int INSERTION_THRESHOLD = 500;

    public CWFluidTankBehaviour tank;
    public int remainingBurnTime;

    Couple<MutableBoolean> sidesToUpdate;
    protected boolean isCreative;
    protected boolean pissedOff;

    double internalTemperature = 0;

    public boolean shouldScan = true;

    private boolean active = false;

    private boolean shouldCheck = false;

    private boolean leaking = false;

    Set<BlockPos> balloonPositions = new HashSet<>();

    Set<BlockPos> volume = new HashSet<>();

    boolean wasProviding = false;

    public boolean alreadyAdded = false;

    private Integer balloonID = null;

    int buffer = 20;
    boolean bufferPulse = false;

    boolean shouldRemove = false;

//    BalloonStructure balloonStructure;

    public BalloonerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = CWFluidTankBehaviour.single(this, 8000);
        behaviours.add(tank);
        super.addBehaviours(behaviours);
    }

    public int getMaxScanRange() {
        return 4096;
        // return PlatformUtils.maxBalloonRange();
    }

    public Set<BlockPos> getBalloonPositions() {
        return balloonPositions;
    }

    public Set<BlockPos> getVolume() {
        return volume;
    }

    public void setShouldRemove() {
        this.shouldRemove = true;
    }

    public void tryCheck() {
        bufferPulse = true;
    }

    public void tryScan() {
        shouldScan = true;
    }

    public boolean canProvide(int size) {
        return size <= getMaxScanRange();
    }

    public BlockPos getWorldPosition() {
        return this.worldPosition;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    public boolean isCreative() {
        return isCreative;
    }

    private int brokenBalloons = 0;

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
//            tickAnimation();
            if (!isVirtual())
                spawnParticles(getFuelQuality(), 1);
            return;
        }
        if (shouldScan) {
            scanBalloon();
            shouldScan = false;
        } else {
            if (shouldCheck) {
                checkBalloon();
                shouldCheck = false;
            }
        }

        if (wasProviding && leaking) {
            leaking = !checkForRepair();
            if (!leaking) {
                brokenBalloons = 0;
            }
        }

        sidesToUpdate.forEachWithContext((update, isFront) -> {
            if (update.isFalse())
                return;
            update.setFalse();
        });

        if (buffer == 0) {
            if (bufferPulse) {
                bufferPulse = false;
                shouldCheck = true;
            }
            buffer = 20;
        } else if (buffer > 0) {
            buffer--;
        }


        if (getRemainingFuel() > 0)
            tank.getPrimaryHandler().shrink(getDrainRate());


        if (volume.isEmpty()) {
            internalTemperature = 0;
        } else {
            internalTemperature = Mth.clamp(internalTemperature + getTemp(), 0, 1);
        }

        if (internalTemperature < 0) {
            internalTemperature = 0;
        } else if (internalTemperature > 1) {
            internalTemperature = 1;
        }

        if (wasProviding && internalTemperature == 0 && leaking) {
            wasProviding = false;
            shouldScan = true;
        }

        LoadedServerShip ship = null;
        if (!level.isClientSide) {
            ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
        }
        if (ship != null) {
            if (!alreadyAdded && balloonID == null) {
                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
                HashSet<Vector3dc> volumePos = new HashSet<>();
                if (!volume.isEmpty()) {
                    for (BlockPos posit : volume) {
                        volumePos.add(VectorConversionsMCKt.toJOMLD(posit));
                    }
                }
                final BalloonCreateData data = new BalloonCreateData(pos, volumePos, speed, internalTemperature, getFuelQuality());
                balloonID = BalloonController.getOrCreate(ship).addBalloon(data);
                alreadyAdded = true;
            }
            if (alreadyAdded && balloonID != null) {
                HashSet<Vector3dc> volumePos = new HashSet<>();
                if (!volume.isEmpty()) {
                    for (BlockPos posit : volume) {
                        volumePos.add(VectorConversionsMCKt.toJOMLD(posit));
                    }
                }
                final BalloonUpdateData data = new BalloonUpdateData(volumePos, speed, internalTemperature, getFuelQuality());
                BalloonController.getOrCreate(ship).updateBalloon(balloonID, data);
            }
            if (this.isRemoved() || shouldRemove) {
                if (balloonID != null) {
                    BalloonController.getOrCreate(ship).removeBalloon(balloonID);
                    balloonID = null;
                    alreadyAdded = false;
                }
            }

        }

    }

    public double getTemp() {
        if (volume.isEmpty()) {
            return 0;
        }
        double temp = 0;
        double passiveCooling = 0;
        if (getFuelQuality().equals(LiquidFuelType.NONE) || Math.abs(speed) <= 16) {
            passiveCooling = -0.0008 * (1.0/volume.size());
        }

        double tempinc = switch (getFuelQuality()) {
            case NONE -> 0.0;
            case STALE -> 0.001;
            case PLAIN -> 0.002;
            case SWEET -> 0.004;
            case GOURMET, EXTRA -> 0.016;
        };
        double vol = 0;
        double volmod = 0;
        if (!volume.isEmpty()) {
            vol = volume.size();
            volmod = 1/vol;
        }

        double throttle = Math.abs(speed) / 256f;

        temp = temp + ((tempinc * throttle) * volmod) + passiveCooling;

        if (leaking) {
            temp = temp - ((0.012 * brokenBalloons) * volmod);
        }

        return temp;
    }

    public boolean checkForRepair() {
        if (balloonPositions.isEmpty()) {
            return false;
        }
        int brokenballoons = 0;
        boolean fixed = true;
        for (BlockPos pos : balloonPositions) {
            if (!level.getBlockState(pos).is(ClockWorkTags.AllBlockTags.BALLOON_BLOCK.tag)) {
                fixed = false;
                brokenballoons++;
            }
        }
        brokenBalloons = brokenballoons;
        return fixed;
    }

    @Override
    public void remove() {
        if (level != null) {
            if (!level.isClientSide) {
                ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
                if (ship != null) {
                    BalloonController controller = BalloonController.getOrCreate(ship);

                    controller.removeBalloon(balloonID);
                }
            }
        }
        super.remove();
    }

    public void scanBalloon() {
        if (level.isClientSide) {
            if (getFuelQuality() == LiquidFuelType.NONE) {
                return;
            }
        }

        EnclosedBalloonScanner scanner = new EnclosedBalloonScanner(level, getMaxScanRange());
        Pair<Set<BlockPos>, Set<BlockPos>> balloonAndSpacePositions = scanner.getEnclosedBalloons(worldPosition.above());

        if (canProvide(balloonAndSpacePositions.left().size()) && !balloonAndSpacePositions.right().isEmpty() && !balloonAndSpacePositions.left().isEmpty()) {
            leaking = false;
            volume = balloonAndSpacePositions.left();
            balloonPositions = balloonAndSpacePositions.right();
            wasProviding = true;
        } else if (wasProviding && !canProvide(balloonAndSpacePositions.left().size()) && balloonAndSpacePositions.left().isEmpty()) {
            leaking = true;
        } else {
            volume.clear();
            balloonPositions.clear();
            leaking = false;
        }


    }

    public void checkBalloon() {
    if (balloonPositions.isEmpty()) {
        shouldScan = true;
        return;
    }
    int brokenballoons = 0;
        for (BlockPos pos : balloonPositions) {
            if (!level.getBlockState(pos).is(ClockWorkTags.AllBlockTags.BALLOON_BLOCK.tag)) {
                brokenballoons++;
                shouldScan = true;
            }
        }
        brokenBalloons = brokenballoons;
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("alreadyAdded", alreadyAdded);
        if (balloonID != null) {
            compound.putInt("balloonID", balloonID);
        }
        compound.putFloat("speed", speed);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        alreadyAdded = compound.getBoolean("alreadyAdded");
        if (compound.contains("balloonID")) {
            balloonID = compound.getInt("balloonID");
        }
        speed = compound.getFloat("speed");
        super.read(compound, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(new TextComponent(spacing).append(new TextComponent("Draining").withStyle(ChatFormatting.GRAY)));
        tooltip.add(new TextComponent(spacing).append(new TextComponent(" " + getDrainRate() + "mb/t ")
                .withStyle(ChatFormatting.AQUA)).append(new TextComponent("with current fuel").withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    protected void spawnParticles(LiquidFuelType heatLevel, double burstMult) {
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
        if (heatLevel.isAtLeast(LiquidFuelType.GOURMET)) {
            level.addParticle(ParticleTypes.PORTAL, v2.x, v2.y, v2.z, 0, yMotion, 0);
        } else if (heatLevel.isAtLeast(LiquidFuelType.SWEET)) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
        } else if (heatLevel.isAtLeast(LiquidFuelType.STALE)) {
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

    public void playSound() {
        level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS,
                .125f + level.random.nextFloat() * .125f, .75f - level.random.nextFloat() * .25f);
    }

    @Override
    public boolean makesHotAir() {
        return true;
    }

    @Override
    public double getHotAirProduction() {
        LiquidFuelType fuelQuality = getFuelQuality();

        double modifier = switch (fuelQuality) {
            case GOURMET, EXTRA -> 1.5;
            case SWEET -> 1.25;
            case PLAIN -> 1;
            case STALE -> .75;
            default -> 0;
        };
        double throttle = Mth.abs(getSpeed() / 256f);
        return modifier * throttle;
    }

    public boolean hasValidFuelType() {
        if (tank.isEmpty())
            return false;

        Fluid fuel = tank.getPrimaryHandler().getFluidType();
        return ClockWorkTags.AllFluidTags.isValidFuel(fuel);
    }

    @Override
    public LiquidFuelType getFuelQuality() {
        if (!hasValidFuelType()) {
            return LiquidFuelType.NONE;
        }

        Fluid fuel = tank.getPrimaryHandler().getFluidType();
        return LiquidFuelType.fromFluid(fuel);
    }

    @Override
    public int getRemainingFuel() {
        if (!hasValidFuelType()) {
            return 0;
        }

        return tank.getPrimaryHandler().getCurrentAmount();
    }

    @Override
    public int getDrainRate() {
        return (int) Math.ceil((2f * getSpeed()) / 256f);
    }

    @Override
    public CWFluidTankBehaviour getFluidTankBehaviour() {
        return tank;
    }

    @Override
    public void setFluidTankBehaviour(CWFluidTankBehaviour tank) {
        this.tank = tank;
    }
}
