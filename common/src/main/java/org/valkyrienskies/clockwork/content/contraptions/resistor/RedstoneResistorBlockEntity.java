package org.valkyrienskies.clockwork.content.contraptions.resistor;

import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.relays.encased.SplitShaftTileEntity;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class RedstoneResistorBlockEntity extends SplitShaftTileEntity implements IHaveGoggleInformation {

    int redstoneLevel = 0;
    int lastChange;
    LerpedFloat clientState;

    public RedstoneResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        redstoneLevel = getPower(level, worldPosition);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putInt("RedstoneLevel", redstoneLevel);
        compound.putInt("ChangeTimer", lastChange);
        super.write(compound, clientPacket);
    }


    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        redstoneLevel = compound.getInt("RedstoneLevel");
        lastChange = compound.getInt("ChangeTimer");
        clientState.chase(redstoneLevel, 0.2f, LerpedFloat.Chaser.EXP);
        super.read(compound, clientPacket);
    }

    private int getPower(Level worldIn, BlockPos pos) {
        int power = 0;
        for (Direction direction : Iterate.directions)
            power = Math.max(worldIn.getSignal(pos.relative(direction), direction), power);
        for (Direction direction : Iterate.directions)
            power = Math.max(worldIn.getSignal(pos.relative(direction), Direction.UP), power);
        return power;
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource()) {
            if (face != getSourceFacing() && getBlockState().getValue(BlockStateProperties.POWERED)) {
                int power = redstoneLevel/15;
                return power;
            }
        }
        return 1;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(componentSpacing.plainCopy().append(Lang.translateDirect("tooltip.analogStrength", this.redstoneLevel)));

        return true;
    }

    public int getState() {
        return redstoneLevel;
    }
}
