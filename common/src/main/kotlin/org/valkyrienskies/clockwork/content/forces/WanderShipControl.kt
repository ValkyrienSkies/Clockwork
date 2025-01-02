package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.minecraft.core.BlockPos
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.util.Vector3icKeyDeserializer
import org.valkyrienskies.clockwork.util.Vector3icKeySerializer
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.mod.common.util.toJOML

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class WanderShipControl : ShipForcesInducer {

    @JsonSerialize(keyUsing = Vector3icKeySerializer::class)
    @JsonDeserialize(keyUsing = Vector3icKeyDeserializer::class)
    val wanderBlocks: HashMap<Vector3i, Double> = HashMap()

    override fun applyForces(physShip: PhysShip) {
        val meanPos: Vector3d = Vector3d()
        for (blockPos in wanderBlocks.keys) {
            meanPos.add(Vector3d(blockPos.x.toDouble() + 0.5, blockPos.y.toDouble() + 0.5, blockPos.z.toDouble() + 0.5))
        }
        meanPos.div(wanderBlocks.size.toDouble())
        meanPos.sub(physShip.transform.positionInShip)
        val sumForce: Double = wanderBlocks.values.sum()
        val force =  Vector3d(0.0, sumForce,0.0).mul(1100.0, Vector3d())

        if (meanPos.isFinite && !meanPos.length().isNaN() && force.isFinite && !force.length().isNaN()) physShip.applyInvariantForceToPos(meanPos, force)
    }

    fun addBlock(blockPos: BlockPos, force: Double) {
        wanderBlocks[blockPos.toJOML()] = force
    }

    fun removeBlock(blockPos: BlockPos) {
        wanderBlocks.remove(blockPos.toJOML())
    }

    companion object {

        fun getOrCreate(ship: LoadedServerShip): WanderShipControl? {
            if (ship.getAttachment(WanderShipControl::class.java) == null) {
                ship.setAttachment(WanderShipControl())
            }
            return ship.getAttachment(WanderShipControl::class.java)
        }
    }
}