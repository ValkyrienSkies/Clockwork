package org.valkyrienskies.clockwork.platform.api

import com.simibubi.create.content.contraptions.IControlContraption
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.gui.AllIcons
import org.valkyrienskies.core.api.ships.Ship

interface ContraptionController : IControlContraption {
    enum class LockedMode(private val icon: AllIcons) : INamedIconOptions {
        FOLLOW_ANGLE(AllIcons.I_ROTATE_PLACE),
        UNLOCKED(AllIcons.I_ROTATE_PLACE_RETURNED),
        LOCKED(AllIcons.I_ROTATE_PLACE);


        private val translationKey: String

        init {
            translationKey = name
        }

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }
}
