package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.content.logistics.gas.filter.GasFilterScreen

class SmartGasFilterScreen(
    private val parent: SmartDuctScreen, private val settings: SmartDuctSettings,
) : GasFilterScreen(
    Component.translatable("vs_clockwork.gui.smart_duct.gas_filter"),
    settings.gasFilter,
    settings.filterBlacklist,
    Component.translatable("vs_clockwork.gui.smart_duct.allow_list_hint"),
    Component.translatable("vs_clockwork.gui.smart_duct.deny_list_hint"),
) {
    override fun onClose() {
        settings.filterBlacklist = blacklist
        minecraft?.setScreen(parent)
    }
}
