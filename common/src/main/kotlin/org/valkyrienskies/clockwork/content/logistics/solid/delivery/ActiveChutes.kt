package org.valkyrienskies.clockwork.content.logistics.solid.delivery

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity

object ActiveChutes {
    private val chutesByDimension = HashMap<ResourceKey<Level>, MutableSet<BlockPos>>()

    fun addChute(level: Level?, pos: BlockPos) {
        if (level == null) return
        chutesByDimension.getOrPut(level.dimension()) { HashSet() }.add(pos.immutable())
    }

    fun removeChute(level: Level?, pos: BlockPos) {
        if (level == null) return
        chutesByDimension[level.dimension()]?.remove(pos)
    }

    fun getChute(level: Level, pos: BlockPos): DeliveryChuteBlockEntity? {
        if (chutesByDimension[level.dimension()]?.contains(pos) != true) return null
        return level.getBlockEntity(pos) as? DeliveryChuteBlockEntity
    }

    fun getSortedChutesWithFrequency(
        level: Level,
        origin: Vector3dc,
        maxDistance: Double,
        frequency: Frequency
    ): List<BlockPos> {
        val positions = chutesByDimension[level.dimension()] ?: return emptyList()
        val maxDistanceSqr = maxDistance * maxDistance

        return positions
            .mapNotNull { pos ->
                val be = level.getBlockEntity(pos) as? DeliveryChuteBlockEntity ?: return@mapNotNull null
                if (be.frequencySlotBehaviour.frequency != frequency) return@mapNotNull null
                val realPos = be.realPos
                val distSqr = realPos.distanceSquared(origin)
                if (distSqr > maxDistanceSqr) null else pos to distSqr
            }
            .sortedBy { it.second }
            .map { it.first }
    }
}
