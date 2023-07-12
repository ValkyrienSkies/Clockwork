package org.valkyrienskies.clockwork.content.munitions.stationary.solver;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.data.ClockWorkTags;
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.FuelBoosterType;
import org.valkyrienskies.clockwork.util.blocktype.IFuelableBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;

import java.util.List;

public class SolverBlockEntity extends KineticBlockEntity implements IFuelableBlockEntity, SmartFluidTankBlockEntity {

    @Nullable
    private Component name;

    private boolean found = true;
    private float targetResistance = 0;
    private BlockPos hitResultPos = BlockPos.ZERO;
    private float destroyProgress = 0;

    private Vec3 start = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;

    public CWFluidTankBehaviour tank;

    private BlockPos previousHitPos = BlockPos.ZERO;


    public SolverBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = CWFluidTankBehaviour.single(this, 8000);
        super.addBehaviours(behaviours);
    }



    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        Vector3dc beamPos = new Vector3d(getBlockPos().getX()+.5, getBlockPos().getY()+1, getBlockPos().getZ()+.5);
        Vector3dc beamEndPos = new Vector3d(getBlockPos().getX()+.5, getBlockPos().getY()+20, getBlockPos().getZ()+.5);
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos());
        if (ship != null) {
            beamPos = ship.getTransform().getShipToWorld().transformPosition(beamPos, new Vector3d());
            beamEndPos = ship.getTransform().getShipToWorld().transformPosition(beamEndPos, new Vector3d());
        }

        Vec3 beamStart = new Vec3(beamPos.x(), beamPos.y(), beamPos.z());
        Vec3 beamEnd = new Vec3(beamEndPos.x(), beamEndPos.y(), beamEndPos.z());

        if (hitResultPos != BlockPos.ZERO) {
            Vector3dc visualHitPos3d = new Vector3d(hitResultPos.getX()+.5, hitResultPos.getY()+.5, hitResultPos.getZ()+.5);

            Ship shipHit = VSGameUtilsKt.getShipObjectManagingPos(level, hitResultPos);

            if (shipHit != null) {
                visualHitPos3d = shipHit.getTransform().getShipToWorld().transformPosition(visualHitPos3d, new Vector3d());
            }

            Vec3 visualHitPos = new Vec3(visualHitPos3d.x(), visualHitPos3d.y(), visualHitPos3d.z());

            start = beamStart;
            end = visualHitPos;

        } else {
            start = beamStart;
            end = beamEnd;
        }
        if (found) {
            BlockHitResult beamResult = RaycastUtilsKt.clipIncludeShips(level, new ClipContext(beamStart, beamEnd, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null));


            if (beamResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = beamResult.getBlockPos();
                BlockState hitState = level.getBlockState(hitPos);

//                targetResistance = hitState.getDestroySpeed(level, hitPos);
                targetResistance = hitState.getBlock().getExplosionResistance();
                hitResultPos = hitPos;
            }
            if (targetResistance > 6001) {
                return;
            }

            if (previousHitPos != BlockPos.ZERO) {
                if (!(previousHitPos.equals(hitResultPos))) {
                    destroyProgress = 0;
                }
            }
            if (Math.abs(getSpeed()) > 64f) {
                if (Math.abs(getSpeed()) <= 192 || getFuelQuality() != LiquidFuelType.GOURMET) {


                    float fuelMod = switch (getFuelQuality()) {
                        case GOURMET -> 1.5f;
                        case SWEET -> 1;
                        case PLAIN -> .5f;
                        default -> 0;
                    };

                    destroyProgress += Mth.clamp(0.5f * Math.abs(getSpeed())/128f * Mth.clamp(1/targetResistance, 0.1, 1) * fuelMod, 0, 2);
                    destroyProgress = Mth.clamp(destroyProgress, 0, 1);
                    level.addParticle(ParticleTypes.DRAGON_BREATH, hitResultPos.getX()+.5, hitResultPos.getY()+.5, hitResultPos.getZ()+.5, Math.random(), Math.random(), Math.random());
                    level.destroyBlockProgress(0, hitResultPos, (int) (destroyProgress * 10));
                    if (destroyProgress >= 1) {
                        level.destroyBlock(hitResultPos, true);
//                    found = false;
                        destroyProgress = 0;
                        hitResultPos = BlockPos.ZERO;
                    }
                } else if (Math.abs(getSpeed()) > 192 && getFuelQuality() == LiquidFuelType.GOURMET) {
                    level.destroyBlock(hitResultPos, true);
                    level.explode(null, hitResultPos.getX(), hitResultPos.getY(), hitResultPos.getZ(), 3, false, Explosion.BlockInteraction.BREAK);
                    destroyProgress = 0;
//                found = false;
                    hitResultPos = BlockPos.ZERO;

                }
            }


            previousHitPos = hitResultPos;
        }


        //beacon rendering


    }

    public Pair<Vec3, Vec3> getLinePoints() {
        return new Pair<>(start, end);
    }

    @Override
    public CWFluidTankBehaviour getFluidTankBehaviour() {
        return tank;
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

        return (int) tank.getPrimaryHandler().getCurrentAmount();
    }

    @Override
    public int getDrainRate() {
        return 100;
    }

    @Override
    public FuelBoosterType getFuelBooster() {
        return null;
    }
}
