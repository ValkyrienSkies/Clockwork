package org.valkyrienskies.clockwork.platform.api;

import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import org.valkyrienskies.core.api.ships.Ship;

import javax.annotation.Nullable;

public interface ContraptionController extends IControlContraption {
    boolean isShipContraptionController();

    @Nullable
    Ship getConnectedShip();

    static enum LockedMode implements INamedIconOptions {
        UNLOCKED(AllIcons.I_ROTATE_PLACE_RETURNED),
        LOCKED(AllIcons.I_ROTATE_PLACE),

        ;

        private String translationKey;
        private AllIcons icon;

        private LockedMode(AllIcons icon) {
            this.icon = icon;
            translationKey = name();
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
