package org.valkyrienskies.clockwork.content.contraptions.phys;

import com.simibubi.create.content.contraptions.components.structureMovement.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import net.minecraft.core.Direction;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;

public interface IAngleLockable {

    boolean locked = false;

    void setLocked(boolean locked);

    boolean getLocked();

    default ValueBoxTransform getLockedSlot(PhysBearingBlockEntity te) {
        return new DirectionalExtenderScrollOptionSlot((state, d) -> {
            Direction.Axis axis = d.getAxis();
            Direction.Axis bearingAxis = state.getValue(BearingBlock.FACING)
                    .getAxis();
            return bearingAxis != axis;
        });
    }

    static enum Locked implements INamedIconOptions {
        UNLOCKED("unlocked", AllIcons.I_ACTIVE),
        LOCKED("locked", AllIcons.I_INSERTED);

        private String name;

        private AllIcons icon;

        Locked(String name, AllIcons icon) {
            this.name = name;
            this.icon = icon;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return "contraptions.movement_mode." + name;
        }
    }
}
