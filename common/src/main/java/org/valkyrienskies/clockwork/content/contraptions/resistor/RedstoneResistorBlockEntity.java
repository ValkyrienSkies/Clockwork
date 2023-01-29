package org.valkyrienskies.clockwork.content.contraptions.resistor;

import com.simibubi.create.content.contraptions.RotationPropagator;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.relays.advanced.sequencer.SequencedGearshiftBlock;
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
import net.minecraft.world.ticks.TickPriority;

import java.util.List;

public class RedstoneResistorBlockEntity extends SplitShaftTileEntity implements IHaveGoggleInformation {

    int redstoneLevel = 0;

    public RedstoneResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getRedstoneLevel() {
        return redstoneLevel;
    }

    public void onRedstoneUpdate(int redstoneLevel) {
        if (redstoneLevel == this.redstoneLevel) return;

        this.redstoneLevel = redstoneLevel;

        this.setChanged();
        this.detachKinetics();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putInt("RedstoneLevel", redstoneLevel);
        super.write(compound, clientPacket);
    }


    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        redstoneLevel = compound.getInt("RedstoneLevel");
        super.read(compound, clientPacket);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (isVirtual())
            return 1;
        if (!hasSource() || face == getSourceFacing())
            return 1;

        return (Math.abs(redstoneLevel-15))/15f;
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
