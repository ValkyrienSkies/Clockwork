package org.valkyrienskies.clockwork.fabric.content.contraptions.components.flap.contraption;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionType;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.AnchoredLighter;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionLighter;
import com.simibubi.create.foundation.utility.NBTHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.fabric.util.contraption.ClockworkContraptionTypes;

import java.util.Queue;
import java.util.Set;

public class FlapContraption extends Contraption {
    protected Direction facing;
    public int offset;

    @Override
    public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
        return searchMovedStructure(world, pos, facing);
    }

    @Override
    public boolean searchMovedStructure(Level world, BlockPos pos, Direction direction) throws AssemblyException {
        return super.searchMovedStructure(world, pos.relative(direction, offset + 1), null);
    }

    @Override
    protected boolean moveBlock(Level world, Direction direction, Queue<BlockPos> frontier,
                                Set<BlockPos> visited) throws AssemblyException {
        return super.moveBlock(world, direction, frontier, visited);
    }

    @Override
    public CompoundTag writeNBT(boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(spawnPacket);
        tag.putInt("facing", facing.get3DDataValue());
        tag.putInt("offset", offset);
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
        facing = Direction.from3DDataValue(tag.getInt("facing"));
        offset = tag.getInt("offset");
        super.readNBT(world, tag, spawnData);
    }

    @Override
    protected ContraptionType getType() {
        return ClockworkContraptionTypes.FLAP;
    }

    @Override
    protected boolean isAnchoringBlockAt(BlockPos pos) {
        return pos.equals(anchor.relative(facing.getOpposite(), offset + 1));
    }

    @Override
    public boolean canBeStabilized(Direction facing, BlockPos localPos) {
        if (BlockPos.ZERO.equals(localPos) || BlockPos.ZERO.equals(localPos.relative(facing)))
            return false;
        return facing.getAxis() == this.facing.getAxis();
    }



    public static FlapContraption assembleFlap(Level world, BlockPos pos, Direction direction) throws AssemblyException {
        FlapContraption contraption = new FlapContraption();
        int flapBlocks = 0;

        contraption.facing = direction;
        if(!contraption.assemble(world, pos)) {
            return null;
        }
        for (int i = 0; i < 16; i++) {
            BlockPos offsetPos = BlockPos.ZERO.relative(direction, i);
            if (contraption.getBlocks()
                    .containsKey(offsetPos))
                continue;
            }

        contraption.startMoving(world);
        contraption.expandBoundsAroundAxis(direction.getAxis());

        return contraption;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public ContraptionLighter<?> makeLighter() {
        return new AnchoredLighter(this);
    }
}
