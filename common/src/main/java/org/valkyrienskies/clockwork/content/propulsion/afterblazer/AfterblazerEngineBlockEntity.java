package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.ClockWorkPackets;
import org.valkyrienskies.clockwork.content.forces.AfterblazerController;
import org.valkyrienskies.clockwork.data.ClockWorkTags;
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.IFuelableBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

public class AfterblazerEngineBlockEntity extends SmartBlockEntity implements IFuelableBlockEntity, SmartFluidTankBlockEntity {

    public CWFluidTankBehaviour tank;

    private int heat = 0;
    private int prevHeat = 0;

    private int redstoneLevel = 0;
    private final Vector2d gimbal = new Vector2d();

    private Integer afterblazerID = null;

    public AfterblazerEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;

        heat += heatUp();

        if (heat > getMaxHeatCapacity()) {
            int amount = 15 * (heat - getMaxHeatCapacity());
            heat -= amount;
        }

        if (hasValidFuelType()) {
            tank.getPrimaryHandler().shrink(getDrainRate());
        }

        if (!level.isClientSide) {
            if (prevHeat != heat) {
                this.setChanged();
            }
        }

        prevHeat = heat;

        if (level.isClientSide) return;

        LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
        if (ship != null) {
            if (afterblazerID == null) {
                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
                Vector3dc dir = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(BlockStateProperties.FACING).getNormal());
                AfterblazerCreateData data = new AfterblazerCreateData(pos, dir, heat, gimbal);
                afterblazerID = AfterblazerController.getOrCreate(ship).addAfterblazer(data);
            } else {
                AfterblazerController.getOrCreate(ship).updateAfterblazer(afterblazerID, new AfterblazerUpdateData(heat, gimbal));
            }
        }
    }

    @Override
    public void remove() {
        if (level != null) {
            if (!level.isClientSide) {
                ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
                if (ship != null) {
                    AfterblazerController controller = AfterblazerController.getOrCreate(ship);

                    controller.removeAfterblazer(afterblazerID);
                }
            }
        }
        super.remove();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        tag.putInt("heat", heat);
        tag.putInt("redstoneLevel", redstoneLevel);
        tag.putDouble("gimbalX", gimbal.x);
        tag.putDouble("gimbalY", gimbal.y);
        if (afterblazerID != null) {
            tag.putInt("afterblazerID", afterblazerID);
        }
        super.write(tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        heat = tag.getInt("heat");
        redstoneLevel = tag.getInt("redstoneLevel");
        gimbal.x = tag.getDouble("gimbalX");
        gimbal.y = tag.getDouble("gimbalY");
        if (tag.contains("afterblazerID")) {
            afterblazerID = tag.getInt("afterblazerID");
        }
    }

    public int heatUp() {
        int maxHeat = getMaxHeatCapacity();

        if (heat >= maxHeat) {
            return 0;
        }
        if (maxHeat == 0) {
            return 0;
        }

        int falloff = (1 - (heat / maxHeat));

        int amount = (5 * getFuelQuality().ordinal()) * falloff;

        //todo booster stuff

        return amount;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = CWFluidTankBehaviour.single(this, 8000);
        behaviours.add(tank);
    }

    public boolean hasValidFuelType() {
        if (tank.isEmpty())
            return false;

        Fluid fuel = tank.getPrimaryHandler().getFluidType();
        return ClockWorkTags.AllFluidTags.isValidFuel(fuel);
    }

    private float getThrottle() {
        return ((float) redstoneLevel) / 15f;
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
        return 4;
    }

    @Override
    public CWFluidTankBehaviour getFluidTankBehaviour() {
        return tank;
    }

    public int getHeat() {
        return heat;
    }

    public void setHeat(int heat) {
        this.heat = heat;
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (getLevel() != null && !getLevel().isClientSide() && !(getLevel() instanceof SchematicWorld)) {
            ClockWorkPackets.sendToNear(getLevel(), getBlockPos(), 64, new AfterblazerStatusPacket(this));
        }
    }

    public int getMaxHeatCapacity() {
        int cap = switch (getFuelQuality()) {
            case NONE -> 0;
            case STALE -> 1000;
            case PLAIN -> 2000;
            case SWEET -> 3000;
            case GOURMET, EXTRA -> 5000;
        };

        cap = (int) (cap * (getThrottle()));
        return cap;
    }

    public void getPower(Level worldIn, BlockPos pos) {
        int power = 0;
        for (Direction direction : Iterate.directions)
            power = Math.max(worldIn.getSignal(pos.relative(direction), direction), power);
        for (Direction direction : Iterate.directions)
            power = Math.max(worldIn.getSignal(pos.relative(direction), Direction.UP), power);

        redstoneLevel = power;
    }

    public int getRedstoneLevel() {
        return redstoneLevel;
    }

    public void setRedstoneLevel(int redstoneLevel) {
        this.redstoneLevel = redstoneLevel;
    }
}
