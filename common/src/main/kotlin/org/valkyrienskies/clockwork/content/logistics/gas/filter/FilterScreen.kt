package org.valkyrienskies.clockwork.content.logistics.gas.filter

import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType

class FilterScreen(
    private val nodeA: DuctNodePos, private val nodeB: DuctNodePos,
    initialFilter: HashSet<GasType>, initialBlacklist: Boolean,
) : GasFilterScreen(Component.translatable("vs_clockwork.gui.gas_filter.title"), initialFilter, initialBlacklist) {
    override fun onClose() {
        ClockworkPackets.sendToServer(FilterClosePacket(nodeA, nodeB, HashSet(filter), blacklist))
        super.onClose()
    }
}
