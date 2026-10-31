package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.gui.AllIcons
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID

enum class GimbalMode(private val icon: AllIcons) : INamedIconOptions {
    LOCKED(AllIcons.I_CONFIG_LOCKED),
    GYROSCOPIC(AllIcons.I_CART_ROTATE),
    UNLOCKED(AllIcons.I_CONFIG_UNLOCKED);

    private val translationKey: String = "$MOD_ID.gimbal_bearing.mode.${name.lowercase()}"

    override fun getIcon(): AllIcons = icon
    override fun getTranslationKey(): String = translationKey
}
