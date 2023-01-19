package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.content.contraptions.relays.encased.SplitShaftTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.util.MinecraftUtil;

import java.util.Set;

public class SequencedSeatBlockEntity extends SplitShaftTileEntity {

    private SequencedSeatRuleList forwardRules = SequencedSeatRuleList.defaultList(Rotation.NONE);
    private SequencedSeatRuleList backwardRules = SequencedSeatRuleList.defaultList(Rotation.CLOCKWISE_180);
    private SequencedSeatRuleList leftRules = SequencedSeatRuleList.defaultList(Rotation.COUNTERCLOCKWISE_90);
    private SequencedSeatRuleList rightRules = SequencedSeatRuleList.defaultList(Rotation.CLOCKWISE_90);


    public SequencedSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (getSourceFacing() != Direction.DOWN)
            return 0;

        if (isVirtual())
            return 1;
        return (!hasSource() || face == getSourceFacing()) ? 1 : getList(face).currentModifier(this);
    }

    public SequencedSeatRuleList getList(Direction face) {
        Direction forward = getBlockState().getValue(SequencedSeatBlock.HORIZONTAL_FACING);
        Rotation rotation = MinecraftUtil.between(forward, face);
        return getList(rotation);
    }

    public SequencedSeatRuleList getList(Rotation rotation) {
        return switch (rotation) {
            case NONE -> forwardRules;
            case CLOCKWISE_90 -> rightRules;
            case CLOCKWISE_180 -> backwardRules;
            case COUNTERCLOCKWISE_90 -> leftRules;
        };
    }

    public Set<InputKey> pressedKeys() {
        return Set.of();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("ForwardRules", forwardRules.serializeNBT());
        compound.put("BackwardRules", backwardRules.serializeNBT());
        compound.put("LeftRules", leftRules.serializeNBT());
        compound.put("RightRules", rightRules.serializeNBT());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        forwardRules.deserializeNBT(compound.getList("ForwardRules", CompoundTag.TAG_COMPOUND));
        backwardRules.deserializeNBT(compound.getList("BackwardRules", CompoundTag.TAG_COMPOUND));
        leftRules.deserializeNBT(compound.getList("LeftRules", CompoundTag.TAG_COMPOUND));
        rightRules.deserializeNBT(compound.getList("RightRules", CompoundTag.TAG_COMPOUND));
    }

    public SequencedSeatRuleList getForwardRules() {
        return forwardRules;
    }

    public SequencedSeatRuleList getBackwardRules() {
        return backwardRules;
    }

    public SequencedSeatRuleList getLeftRules() {
        return leftRules;
    }

    public SequencedSeatRuleList getRightRules() {
        return rightRules;
    }

    public void updateRules(
            SequencedSeatRuleList forwardRules,
            SequencedSeatRuleList backwardRules,
            SequencedSeatRuleList leftRules,
            SequencedSeatRuleList rightRules) {
        this.forwardRules = forwardRules;
        this.backwardRules = backwardRules;
        this.leftRules = leftRules;
        this.rightRules = rightRules;
        sendData();
        setChanged();
    }
}
