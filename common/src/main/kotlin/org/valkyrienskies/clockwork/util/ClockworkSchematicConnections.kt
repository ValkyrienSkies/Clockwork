package org.valkyrienskies.clockwork.util

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalLinearBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftBlockEntity
import org.valkyrienskies.clockwork.util.universal_joint.IUniversalJoint
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.ArrayDeque

object ClockworkSchematicConnections {
    @JvmStatic
    fun expand(level: ServerLevel, initial: List<Long>): List<Long> {
        val included = initial.toMutableSet()
        val queue = ArrayDeque(initial)
        fun include(id: Long?) {
            if (id == null || id < 0 || level.shipObjectWorld.allShips.getById(id) == null || !included.add(id)) return
            queue.add(id)
        }
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            level.gtpa.getAllConnectedShips(id).forEach(::include)
            val ship = level.shipObjectWorld.allShips.getById(id) ?: continue
            ship.activeChunksSet.forEach { x, z ->
                if (!level.hasChunk(x, z)) return@forEach
                for (be in level.getChunk(x, z).blockEntities.values) {
                    val other = (be as? IUniversalJoint)?.connectedJoint?.pos
                        ?: (be as? UniversalShaftBlockEntity)?.connectedPos
                    other?.let { include(level.getShipManagingPos(it)?.id) }
                    if (be is PhysicalLinearBlockEntity)
                        ClockworkBlockReferences.positions(be.saveWithoutMetadata()).values.forEach {
                            include(level.getShipManagingPos(it)?.id)
                        }
                }
            }
            for (link in WandLinks.get(level).links.values) {
                if (link.a.shipId == id) include(link.b.shipId)
                if (link.b.shipId == id) include(link.a.shipId)
            }
            for (link in HingeLinks.get(level).links.values.filter { it.connected }) {
                if (link.a.shipId == id) include(link.b.shipId)
                if (link.b.shipId == id) include(link.a.shipId)
            }
        }
        return included.toList()
    }
}
