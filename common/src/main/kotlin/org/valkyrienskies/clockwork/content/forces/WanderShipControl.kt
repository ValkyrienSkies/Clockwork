package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.curiosities.IWanderliteBlock
import org.valkyrienskies.clockwork.util.Vector3icKeyDeserializer
import org.valkyrienskies.clockwork.util.Vector3icKeySerializer
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.util.toJOML
import java.util.concurrent.ConcurrentHashMap

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class WanderShipControl : ShipPhysicsListener {

    @JsonSerialize(keyUsing = Vector3icKeySerializer::class)
    @JsonDeserialize(keyUsing = Vector3icKeyDeserializer::class)
    val wanderBlocks: ConcurrentHashMap<Vector3i, Double> = ConcurrentHashMap()

    @JsonIgnore
    private val scannedChunks = HashSet<Long>()

    @JsonIgnore
    private var needsChunkScan = true

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val center = Vector3d()
        var totalMass = 0.0
        for ((pos, mass) in wanderBlocks) {
            if (!mass.isFinite() || mass <= 0.0) continue
            center.add((pos.x + 0.5) * mass, (pos.y + 0.5) * mass, (pos.z + 0.5) * mass)
            totalMass += mass
        }
        if (totalMass == 0.0 || !totalMass.isFinite()) return

        center.div(totalMass)
        val gravity = physLevel.aerodynamicUtils.getAtmosphereForDimension(physLevel.dimension).third
        val lift = totalMass * gravity * ClockworkConfig.SERVER.wanderOreForceMultiplier
        if (lift.isFinite() && lift != 0.0 && center.isFinite) {
            physShip.applyWorldForceToModelPos(Vector3d(0.0, lift, 0.0), center)
        }
    }

    fun updateBlock(pos: BlockPos, state: BlockState) {
        val key = pos.toJOML()
        val mass = liftMass(state)
        if (mass == null) {
            wanderBlocks.remove(key)
        } else {
            wanderBlocks[key] = mass
        }
    }

    fun onShipLoad() {
        scannedChunks.clear()
        needsChunkScan = true
    }

    fun reconcileBlocks(level: ServerLevel, ship: LoadedServerShip) {
        if (!needsChunkScan || ship.chunkClaimDimension != level.dimensionId) return

        val activeChunks = ship.activeChunksSet
        var waitingForChunk = false
        activeChunks.forEach { x, z ->
            val key = ChunkPos.asLong(x, z)
            if (key in scannedChunks) return@forEach
            val chunk = level.chunkSource.getChunkNow(x, z)
            if (chunk == null) {
                waitingForChunk = true
            } else {
                reconcileChunk(chunk)
                scannedChunks.add(key)
            }
        }
        if (!waitingForChunk && scannedChunks.isNotEmpty()) {
            wanderBlocks.keys.removeIf { !activeChunks.contains(it.x shr 4, it.z shr 4) }
            scannedChunks.clear()
            needsChunkScan = false
        }
    }

    private fun reconcileChunk(chunk: LevelChunk) {
        val found = HashSet<Vector3i>()
        val chunkPos = chunk.pos
        for ((index, section) in chunk.sections.withIndex()) {
            if (!section.maybeHas { it.block is IWanderliteBlock }) continue
            val baseY = chunk.getSectionYFromSectionIndex(index) shl 4
            for (y in 0..15) for (z in 0..15) for (x in 0..15) {
                val mass = liftMass(section.getBlockState(x, y, z)) ?: continue
                val pos = Vector3i(chunkPos.minBlockX + x, baseY + y, chunkPos.minBlockZ + z)
                found.add(pos)
                wanderBlocks[pos] = mass
            }
        }
        wanderBlocks.keys.removeIf { it.x shr 4 == chunkPos.x && it.z shr 4 == chunkPos.z && it !in found }
    }

    companion object {
        private fun liftMass(state: BlockState): Double? {
            if (state.block !is IWanderliteBlock) return null
            return BlockStateInfo.get(state)?.first?.takeIf { it.isFinite() && it > 0.0 }
        }

        fun getOrCreate(ship: LoadedServerShip): WanderShipControl {
            return ship.getAttachment(WanderShipControl::class.java)
                ?: WanderShipControl().also { ship.setAttachment(it) }
        }
    }
}
