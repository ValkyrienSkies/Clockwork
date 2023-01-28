package org.valkyrienskies.clockwork.content.contraptions.propellor;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionType;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.AnchoredLighter;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionLighter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.valkyrienskies.clockwork.ClockWorkContraptions;

import java.util.Queue;
import java.util.Set;

public class PropellorContraption extends Contraption {

    public int offset;
    protected int sailBlocks;
    protected Direction facing;

    public PropellorContraption() {}

    public PropellorContraption(Direction facing) {
        this.facing = facing;
    }

    public static PropellorContraption assembleProp(Level world, BlockPos pos, Direction direction) throws AssemblyException {
        PropellorContraption contraption = new PropellorContraption();
        int flapBlocks = 0;

        contraption.facing = direction;
        if (!contraption.assemble(world, pos)) {
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
//    @Override
//    public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
//        BlockPos offset = pos.relative(facing);
//        if (!searchMovedStructure(world, offset, null))
//            return false;
//        startMoving(world);
//        expandBoundsAroundAxis(facing.getAxis());
//        if (sailBlocks < 2)
//            throw AssemblyException.notEnoughSails(sailBlocks);
//        if (blocks.isEmpty())
//            return false;
//        return true;
//    }

    @Override
    public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
        return searchMovedStructure(world, pos, facing);
    }
    @Override
    protected boolean moveBlock(Level world, Direction direction, Queue<BlockPos> frontier,
                                Set<BlockPos> visited) throws AssemblyException {
        return super.moveBlock(world, direction, frontier, visited);
    }

    @Override
    public boolean searchMovedStructure(Level world, BlockPos pos, Direction direction) throws AssemblyException {
        return super.searchMovedStructure(world, pos.relative(direction, offset + 1), null);
    }

    @Override
    protected ContraptionType getType() {
        return ClockWorkContraptions.PROPELLOR;
    }

    @Override
    protected boolean isAnchoringBlockAt(BlockPos pos) {
        return pos.equals(anchor.relative(facing.getOpposite(), offset + 1));
    }

    @Override
    public void addBlock(BlockPos pos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> capture) {
        BlockPos localPos = pos.subtract(anchor);
        if (!getBlocks().containsKey(localPos) && AllTags.AllBlockTags.WINDMILL_SAILS.matches(capture.getKey().state))
            sailBlocks++;
        super.addBlock(pos, capture);
    }

    @Override
    public CompoundTag writeNBT(boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(spawnPacket);
        tag.putInt("Sails", sailBlocks);
        tag.putInt("Facing", facing.get3DDataValue());
        tag.putInt("Offset", offset);
        return tag;
    }

    @Override
    public void readNBT(Level world, CompoundTag tag, boolean spawnData) {
        sailBlocks = tag.getInt("Sails");
        facing = Direction.from3DDataValue(tag.getInt("Facing"));
        offset = tag.getInt("Offset");
        super.readNBT(world, tag, spawnData);
    }

    public int getSailBlocks() {
        return sailBlocks;
    }

    public Direction getFacing() {
        return facing;
    }

    @Override
    public boolean canBeStabilized(Direction facing, BlockPos localPos) {
        if (facing.getOpposite() == this.facing && BlockPos.ZERO.equals(localPos))
            return false;
        return facing.getAxis() == this.facing.getAxis();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public ContraptionLighter<?> makeLighter() {
        return new AnchoredLighter(this);
    }
}