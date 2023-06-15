package org.valkyrienskies.clockwork.content.materials.solids.colorblock;

import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkPackets;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.BlockEntityColorPacket;

import java.util.List;

public class ColorBlockEntity extends SmartTileEntity {
    private int color = -1;

    public ColorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {}

    @Override
    public void write(CompoundTag tag, boolean client) {
        if (this.color != -1)
            tag.putInt("Clockwork$color", this.color);

        if (getLevel() != null && !getLevel().isClientSide()) {
            ClockWorkPackets.sendToNear(getLevel(), getBlockPos(), 64, new BlockEntityColorPacket(this));
        }
        super.writeSafe(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("Clockwork$color"))
            this.color = tag.getInt("Clockwork$color");

        if (getLevel() != null && !getLevel().isClientSide() && !(getLevel() instanceof SchematicWorld)) {
            ClockWorkPackets.sendToNear(getLevel(), getBlockPos(), 64, new BlockEntityColorPacket(this));
        }
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int rgb) {
        this.color = rgb;
        this.setChanged();
    }

    public void clearColor() {
        this.color = -1;
        this.setChanged();
    }
}
