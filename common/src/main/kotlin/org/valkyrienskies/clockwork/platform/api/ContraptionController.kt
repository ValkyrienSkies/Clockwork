package org.valkyrienskies.clockwork.platform.api

import com.simibubi.create.content.contraptions.IControlContraption
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.gui.AllIcons
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.core.api.ships.Ship

interface ContraptionController : IControlContraption {
    enum class LockedMode(private val icon: AllIcons) : INamedIconOptions {
        FOLLOW_ANGLE(AllIcons.I_ROTATE_PLACE),
        UNLOCKED(AllIcons.I_ROTATE_PLACE_RETURNED),
        //TODO idk how to safely remove this
        LOCKED(AllIcons.I_ROTATE_PLACE);


        private val translationKey: String

        init {
            translationKey = "$MOD_ID.phys_bearing.rotation_mode.${name.lowercase()}"
        }

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }
}
