package org.valkyrienskies.clockwork.kelvin.api.edges

import com.simibubi.create.foundation.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterScreenOpenPacket
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorScreen
import org.valkyrienskies.clockwork.kelvin.api.DuctEdge
import org.valkyrienskies.clockwork.kelvin.api.GasType

/**
 * Represents a filtered connection in the graph. Filtered connections only allow certain gases to flow through.
 */
interface FilteredEdge: DuctEdge {
    /**
     * The current filter set for this connection. Behavior determined by the [blacklist] variable.
     */
    val filter : HashSet<GasType>

    /**
     * Determines whether this connection's filter is a Whitelist (false) or a Blacklist (true).
     *
     * A **Whitelist** means that only the specified gases are allowed to flow through the connection.
     *
     * A **Blacklist** means that all gases are allowed to flow through the connection *except* for the specified gases.
     */
    var blacklist: Boolean

    fun modFilter(newFilter: HashSet<GasType>, isBlacklist: Boolean) {
        this.filter.clear()
        this.filter.addAll(newFilter)
        this.blacklist = isBlacklist
    }

    override fun interact(player: ServerPlayer): Boolean {
        ClockworkPackets.sendTo(FilterScreenOpenPacket(nodeA, nodeB, filter, blacklist), player)
        return true
    }

}