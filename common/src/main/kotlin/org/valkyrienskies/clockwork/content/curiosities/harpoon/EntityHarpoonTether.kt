package org.valkyrienskies.clockwork.content.curiosities.harpoon

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotPhysics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.api.chains.ChainCreateData
import org.valkyrienskies.core.api.chains.ChainEnd
import org.valkyrienskies.core.api.chains.ChainEndpoint
import org.valkyrienskies.core.api.chains.ChainId
import org.valkyrienskies.core.api.chains.ChainLifecycle
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML

@OptIn(GameTickOnly::class)
internal class EntityHarpoonTether {
    var chainId: ChainId? = null
    private var lastEnd: Vec3? = null
    private var lastLength = Double.NaN
    private var missingTargetTicks = 0

    fun clear(level: ServerLevel) {
        val id = chainId ?: return
        level.shipObjectWorld.deleteChain(id)
        chainId = null
        lastEnd = null
        lastLength = Double.NaN
        WandLinks.get(level).setDirty()
    }

    fun tick(level: ServerLevel, link: WandLinks.Link): Boolean {
        val world = level.shipObjectWorld
        val start = link.a.world(level)
        val end = link.b.world(level)
        val target = link.b.entity(level)
        if (target != null && !target.isAlive) return false
        val ship = if (link.a.shipId >= 0) world.allShips.getById(link.a.shipId) ?: return false else null
        val sourceLoaded = level.hasChunkAt(link.a.pos)
        if (sourceLoaded && (level.getBlockEntity(link.a.pos) as? SiphonHarpoonBlockEntity)?.ropeId != link.id) return false
        if (start == null || end == null || !sourceLoaded || (ship != null && ship.bodyId == null)) {
            clear(level)
            link.previousDistance = null
            link.suspended = true
            link.stress = 0f
            if (end == null && sourceLoaded) missingTargetTicks++
            return missingTargetTicks <= 40
        }
        missingTargetTicks = 0
        link.suspended = false
        val distance = start.distanceTo(end)
        val speed = (distance - (link.previousDistance ?: distance)) * 20
        link.previousDistance = distance
        val stress = RopeKnotPhysics.stressRatio(distance - link.length, speed)
        if (!distance.isFinite() || stress > 1.0 || distance > HarpoonProjectiles.maxRange) return false
        link.stress = stress.toFloat()

        val endPoint = ChainEndpoint.World(end.toJOML())
        val chain = chainId?.let(world::getChainById) ?: run {
            val source = ship?.bodyId?.let { ChainEndpoint.Body(it, link.a.local().toJOML()) }
                ?: ChainEndpoint.World(start.toJOML())
            val seedEnd = if (distance < 0.25) start.add(0.0, 0.25, 0.0) else end
            world.createChain(ChainCreateData(level.dimensionId, start = source, end = ChainEndpoint.World(seedEnd.toJOML()),
                targetSpacing = distance + 0.5, particleRadius = 0.0625, collisionMask = 0)).also {
                chainId = it.id
                lastEnd = seedEnd
                lastLength = Double.NaN
                WandLinks.get(level).setDirty()
            }
        }
        if (chain.status.hasSnapped || chain.status.lifecycle == ChainLifecycle.FAILED ||
            chain.status.lifecycle == ChainLifecycle.REMOVED) return false
        if (chain.status.lifecycle != ChainLifecycle.ACTIVE) return true
        if (lastEnd != end) {
            chain.setEndpoint(ChainEnd.END, endPoint)
            chain.nodeIds.lastOrNull()?.let(world::getParticleById)?.teleport(endPoint.position)
            lastEnd = end
        }
        if (lastLength != link.length) {
            chain.setTargetLength(link.length)
            lastLength = link.length
        }
        return true
    }
}
