package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.SplitShaftTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.integration.cc.ComputerAttachmentHandler;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.util.MinecraftUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SequencedSeatBlockEntity extends SplitShaftTileEntity {

    private SequencedSeatRuleList forwardRules = SequencedSeatRuleList.defaultList(Rotation.NONE);
    private SequencedSeatRuleList backwardRules = SequencedSeatRuleList.defaultList(Rotation.CLOCKWISE_180);
    private SequencedSeatRuleList leftRules = SequencedSeatRuleList.defaultList(Rotation.COUNTERCLOCKWISE_90);
    private SequencedSeatRuleList rightRules = SequencedSeatRuleList.defaultList(Rotation.CLOCKWISE_90);
    private Set<InputKey> pressedKeys = Set.of();
    private float[] degreesAwayFromBase = new float[4];
    private float[] lastModifier = new float[4];

    public final ComputerAttachmentHandler computerHandler = new ComputerAttachmentHandler();

    public SequencedSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;

        for (int i = 0; i < 4; i++) {
            Direction dir = Direction.values()[i + 2];
            float modifier = getRotationSpeedModifier(dir);
            degreesAwayFromBase[i] += KineticTileEntity.convertToAngular(modifier * speed);

            if (modifier != lastModifier[i]) {
                detachKinetics();
                attachKinetics();
            }

            lastModifier[i] = modifier;

            if (degreesAwayFromBase[i] > 360)
                degreesAwayFromBase[i] -= 360;

            if (degreesAwayFromBase[i] < 0)
                degreesAwayFromBase[i] += 360;
        }
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (isVirtual() || !hasSource())
            return 1;

        if (getSourceFacing() != Direction.DOWN)
            return 0;

        if (face == getSourceFacing())
            return 1;

        return getList(face).currentModifier(this, face);
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
        return pressedKeys;
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
        detachKinetics();
        attachKinetics();
    }

    public void updateInput(Set<InputKey> pressedKeys) {
        if (this.pressedKeys.equals(pressedKeys))
            return;

        if (!level.isClientSide)
            if (PlatformUtils.isModLoaded("computercraft")) {
                List<String> event = new ArrayList<>();

                this.pressedKeys.forEach(key -> event.add(key.name()));

                this.computerHandler.sendEvent("command_seat_keys", event);
            }

        this.pressedKeys = pressedKeys;
    }

    public float getDegreesAwayFromBase(Direction direction) {
        return degreesAwayFromBase[direction.ordinal() - 2];
    }
}
