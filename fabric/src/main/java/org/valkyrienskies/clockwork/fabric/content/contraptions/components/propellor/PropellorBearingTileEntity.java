package org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor;

import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.fabric.config.AllClockworkConfigs;

import java.util.List;

public class PropellorBearingTileEntity extends MechanicalBearingTileEntity {

    protected ScrollOptionBehaviour<RotationDirection> movementDirection;

    protected boolean running;
    protected boolean assembleNextTick;
    protected boolean queuedReassembly;

    public PropellorBearingTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        boolean cancelAssembly = assembleNextTick;
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = cancelAssembly;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide())
            return;
        if (!queuedReassembly)
            return;
        queuedReassembly = false;
        if (!running)
            assembleNextTick = true;
    }

    public void disassembleForMovement() {
        if (!running)
            return;
        disassemble();
        queuedReassembly = true;
    }

    public float getGeneratedPropulsion() {
        if (!running)
            return 0;
        int sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks()
                / AllClockworkConfigs.SERVER.kinetics.propellorRPMPerSail.get();
        return Mth.clamp(sails, 1, 16) * getAngleSpeedDirection();
    }

    protected boolean isPropellor() {
        return true;
    }

    protected float getAngleSpeedDirection() {
        RotationDirection rotationDirection = RotationDirection.values()[movementDirection.getValue()];
        return (rotationDirection == RotationDirection.CLOCKWISE ? 1 : -1);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("QueueAssembly", queuedReassembly);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        queuedReassembly = compound.getBoolean("QueueAssembly");
        super.read(compound, clientPacket);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.remove(movementMode);
        movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class,
                Lang.translateDirect("contraptions.propellor.rotation_direction"), this, getMovementModeSlot());
        movementDirection.requiresWrench();
        movementDirection.withCallback($ -> onDirectionChanged());
        behaviours.add(movementDirection);
    }

    private void onDirectionChanged() {
        if (!running)
            return;
        if (!level.isClientSide)
            updateGeneratedRotation();
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    public static enum RotationDirection implements INamedIconOptions {

        CLOCKWISE(AllIcons.I_REFRESH), COUNTER_CLOCKWISE(AllIcons.I_ROTATE_CCW),

        ;

        private String translationKey;
        private AllIcons icon;

        private RotationDirection(AllIcons icon) {
            this.icon = icon;
            translationKey = "generic." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

    }

}