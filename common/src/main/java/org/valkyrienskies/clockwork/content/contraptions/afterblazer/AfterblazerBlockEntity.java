package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.content.forces.AfterblazerController;
import org.valkyrienskies.clockwork.data.ClockWorkTags;
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.*;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.lang.Math;
import java.util.List;
import java.util.Random;

public class AfterblazerBlockEntity extends SmartTileEntity implements IFuelableTileEntity, SmartFluidTankBlockEntity, IHaveGoggleInformation {

        public static final int MAX_HEAT_CAPACITY = 10000;
        protected int remainingBurnTime;

        public CWFluidTankBehaviour tank;

        protected LerpedFloat headAnimation;
        protected LerpedFloat headAngle;
        protected boolean isCreative;
        protected boolean goggles;
        protected boolean hat;
        protected boolean pissedOff;

        protected int redstoneLevel;
        protected boolean isPowered;

        Vector3d northNorm;

        private Integer afterblazerID = null;
        protected boolean alreadyAdded = false;
        private Vector2d gimbalRotation = new Vector2d();
        public AfterblazerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
            headAnimation = LerpedFloat.linear();
            headAngle = LerpedFloat.angular();
            goggles = false;
            redstoneLevel = 0;

            headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(AfterblazerBlock.FACING)
                    .orElse(Direction.SOUTH)) + 180) % 360);
        }



        public void setGimbal(double pitch, double yaw) {
            double tPitch = Mth.clamp(pitch, -22.5, 22.5);
            double tYaw = Mth.clamp(yaw, -22.5, 22.5);
            gimbalRotation.set(tPitch, tYaw);
        }

        public Vector2d getGimbalVector() {
            return gimbalRotation;
        }

        public double getGimbalPitch() {
            return gimbalRotation.x;
        }

        public double getGimbalYaw() {
            return gimbalRotation.y;
        }

        @Override
        public void tick() {
            super.tick();

            if (level.isClientSide) {
                tickAnimation();
                if (!isVirtual())
                    spawnParticles(getFuelQuality(), 1);
                return;
            }
            if (isCreative)
                return;

            if (getRemainingFuel() > 0)
                tank.getPrimaryHandler().shrink(getDrainRate() * (long) (getThrustPercentage()));

            isPowered = getBlockState().getValue(BlockStateProperties.POWERED);

            redstoneLevel = getPower(level, getBlockPos());


            LoadedServerShip ship = null;
            if (!level.isClientSide) {
                if (VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos()) != null) {
                    ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
                }
            }

            if (ship != null) {
                if (!alreadyAdded && afterblazerID == null) {
                    Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
                    final AfterblazerCreateData data = new AfterblazerCreateData(getBlockState().getValue(BlockStateProperties.FACING), remainingBurnTime, getFuelQuality(), redstoneLevel, pos, gimbalRotation);
                    afterblazerID = AfterblazerController.getOrCreate(ship).addAfterblazer(data);
                    alreadyAdded = true;
                }
                if (alreadyAdded && afterblazerID != null) {
                    final AfterblazerUpdateData data = new AfterblazerUpdateData(remainingBurnTime, getFuelQuality(), redstoneLevel, gimbalRotation);
                    AfterblazerController.getOrCreate(ship).updateAfterblazer(afterblazerID, data);
                }
                if (this.isRemoved()) {
                    if (afterblazerID != null) {
                        AfterblazerController.getOrCreate(ship).removeAfterblazer(afterblazerID);
                        afterblazerID = null;
                        alreadyAdded = false;
                    }
                }

            }

            if (getRemainingFuel() > 0)
                return;

        }

        public int getPower(Level worldIn, BlockPos pos) {
            int power = 0;
            for (Direction direction : Iterate.directions)
                power = Math.max(worldIn.getSignal(pos.relative(direction), direction), power);
            for (Direction direction : Iterate.directions)
                power = Math.max(worldIn.getSignal(pos.relative(direction), Direction.UP), power);
            return power;
        }

        public double getThrustPercentage () {
            if (redstoneLevel == 15) {
                return 100/100.0;
            } else if (redstoneLevel == 14) {
                return 93/100.0;
            } else if (redstoneLevel == 13) {
                return 86/100.0;
            } else if (redstoneLevel == 12) {
                return 79/100.0;
            } else if (redstoneLevel == 11) {
                return 72/100.0;
            } else if (redstoneLevel == 10) {
                return 65/100.0;
            } else if (redstoneLevel == 9) {
                return 58/100.0;
            } else if (redstoneLevel == 8) {
                return 51/100.0;
            } else if (redstoneLevel == 7) {
                return 44/100.0;
            } else if (redstoneLevel == 6) {
                return 37/100.0;
            } else if (redstoneLevel == 5) {
                return 30/100.0;
            } else if (redstoneLevel == 4) {
                return 23/100.0;
            } else if (redstoneLevel == 3) {
                return 16/100.0;
            } else if (redstoneLevel == 2) {
                return 9/100.0;
            } else if (redstoneLevel == 1) {
                return 2/100.0;
            } else {
                return 0;
            }
        }
        @Environment(EnvType.CLIENT)
        private void tickAnimation() {
            boolean active = getRemainingFuel() > 0;


                float target = 0;
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && !player.isInvisible()) {
                    double x;
                    double z;
                    if (isVirtual()) {
                        x = -4;
                        z = -10;
                    } else {
                        x = player.getX();
                        z = player.getZ();
                    }
                    double dx = x - (getBlockPos().getX() + 0.5);
                    double dz = z - (getBlockPos().getZ() + 0.5);
                    target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
                }
                target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
                headAngle.chase(target, .25f, LerpedFloat.Chaser.exp(5));
                headAngle.tickChaser();




            headAnimation.chase(active ? 1 : 0, .25f, LerpedFloat.Chaser.exp(.25f));
            headAnimation.tickChaser();
        }

        @Override
        public void addBehaviours(List<TileEntityBehaviour> behaviours) {
            tank = CWFluidTankBehaviour.single(this, 8000);
        }

        @Override
        public void write(CompoundTag compound, boolean clientPacket) {
            if (goggles)
                compound.putBoolean("Goggles", true);
            if (hat)
                compound.putBoolean("TrainHat", true);
            if (afterblazerID != null) {
                compound.putInt("ID", afterblazerID);
            }
            super.write(compound, clientPacket);
        }

        @Override
        protected void read(CompoundTag compound, boolean clientPacket) {
            goggles = compound.contains("Goggles");
            hat = compound.contains("TrainHat");
            afterblazerID = compound.contains("ID") ? compound.getInt("ID") : null;
            super.read(compound, clientPacket);
        }


        /**
         * @return true if the heater updated its burn time and an item should be
         *         consumed
         */


//        protected void playSound() {
//            level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS,
//                    .125f + level.random.nextFloat() * .125f, .75f - level.random.nextFloat() * .25f);
//        }

        public double getParticleThrust() {
            if (!getFuelQuality().isAtLeast(LiquidFuelType.STALE)) {
                return 0;
            }
            double thrust = 1;

//            if (getRemainingBurnTime() > 5000) {
//
//                thrust = 1;
//            } else if (getRemainingBurnTime() > 1000) {
//                thrust = 0.75;
//            } else if (getRemainingBurnTime() > 500) {
//                thrust = 0.5;
//            } else if (getRemainingBurnTime() > 250) {
//                thrust = 0.25;
//            } else {
//                thrust = 0;
//            }

            return thrust * getThrustPercentage();
        }

        protected void spawnParticles(LiquidFuelType fuelQuality, double burstMult) {
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
            if (fuelQuality.isAtLeast(LiquidFuelType.GOURMET)) {
                level.addParticle(ParticleTypes.PORTAL, v2.x, v2.y, v2.z, 0, yMotion, 0);
            } else if (fuelQuality.isAtLeast(LiquidFuelType.SWEET)) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
            } else if (fuelQuality.isAtLeast(LiquidFuelType.STALE)) {
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

    public boolean hasValidFuelType() {
        if (tank.isEmpty())
            return false;

        Fluid fuel = tank.getPrimaryHandler().getFluidType();

        if (fuel.is(ClockWorkTags.AllFluidTags.STALE.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.PLAIN.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.SWEET.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.EXTRA.tag))
            return true;
        if (fuel.is(ClockWorkTags.AllFluidTags.GOURMET.tag))
            return true;

        return false;
    }

    @Override
    public LiquidFuelType getFuelQuality() {
        if (!hasValidFuelType()) {
            return LiquidFuelType.NONE;
        }
        Fluid fuel = tank.getPrimaryHandler().getFluidType();
        if (fuel.is(ClockWorkTags.AllFluidTags.STALE.tag)) {
            return LiquidFuelType.STALE;
        } else if (fuel.is(ClockWorkTags.AllFluidTags.PLAIN.tag)) {
            return LiquidFuelType.PLAIN;
        } else if (fuel.is(ClockWorkTags.AllFluidTags.SWEET.tag)) {
            return LiquidFuelType.SWEET;
        } else if (fuel.is(ClockWorkTags.AllFluidTags.GOURMET.tag)) {
            return LiquidFuelType.GOURMET;
        } else {
            return LiquidFuelType.EXTRA;
        }
    }

    @Override
    public int getRemainingFuel() {
        if (!hasValidFuelType()) {
            return 0;
        }

        return (int) tank.getPrimaryHandler().getAmount();
    }

    @Override
    public int getDrainRate() {
        return 10 * (int)(getThrustPercentage());
    }

    @Override
    public FuelBoosterType getFuelBooster() {
        return null;
    }

    @Override
    public CWFluidTankBehaviour getFluidTankBehaviour() {
        return tank;
    }

    }
