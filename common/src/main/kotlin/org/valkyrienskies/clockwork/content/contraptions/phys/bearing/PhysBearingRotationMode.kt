package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.gui.AllIcons
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID

enum class PhysBearingRotationMode(private val icon: AllIcons) : INamedIconOptions {
    FOLLOW_ANGLE(AllIcons.I_ROTATE_PLACE),
    UNLOCKED(AllIcons.I_ROTATE_PLACE_RETURNED);

    private val translationKey = "$MOD_ID.phys_bearing.rotation_mode.${name.lowercase()}"

    override fun getIcon(): AllIcons = icon

    override fun getTranslationKey(): String = translationKey
}
