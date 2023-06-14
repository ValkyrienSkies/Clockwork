package org.valkyrienskies.clockwork.content.materials.solids.colorblock;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkPackets;
import org.valkyrienskies.clockwork.util.render.ColorBlockEntityPacket;

import java.util.List;

public class ColorBlockEntity extends SmartTileEntity {
    private int color = -1;

    public ColorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {}

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        if (this.color != -1)
            tag.putInt("Clockwork$color", this.color);

        super.write(tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("Clockwork$color"))
            this.color = tag.getInt("Clockwork$color");
    }

    public void setColor(int rgb) {
        this.color = rgb;
        this.setChanged();
    }

    public int getColor() {
        return this.color;
    }

    public void clearColor() {
        this.color = -1;
        this.setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();

        assert level != null;
        if (level.isClientSide)
            return;

        ColorBlockEntityPacket packet = new ColorBlockEntityPacket(worldPosition, this.color);
        ClockWorkPackets.sendToAllPlayers(packet, (ServerLevel) level);
    }
}
