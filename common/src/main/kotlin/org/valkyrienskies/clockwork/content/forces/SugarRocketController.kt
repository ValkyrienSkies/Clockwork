package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.mod.api.toJOMLd
import org.valkyrienskies.mod.common.util.toJOML
import java.util.concurrent.ConcurrentLinkedQueue

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class SugarRocketController : ShipForcesInducer {
    val newRockets = ConcurrentLinkedQueue<Pair<Vector3i, Vector3d>>()
    val removedRockets = ConcurrentLinkedQueue<Vector3i>()
    val burningRockets: HashMap<Vector3i, Vector3d> = HashMap()

    override fun applyForces(physShip: PhysShip) {
        while (newRockets.isNotEmpty()) {
            val rocket = newRockets.poll()
            burningRockets[rocket.first] = rocket.second
            println("added rocket at ${rocket.first} with force ${rocket.second}")
        }
        while (removedRockets.isNotEmpty()) {
            val rocket = removedRockets.poll()
            burningRockets.remove(rocket)
            println("removed rocket at $rocket")
        }
        burningRockets.forEach { (pos, force) ->
            val shipPos = Vector3d(pos).add(0.5, 0.5, 0.5, Vector3d()).sub(physShip.transform.positionInShip)
            physShip.applyRotDependentForceToPos(force, shipPos)
        }
    }

    fun addRocket(pos: BlockPos, force: Double, direction: Direction) {
        newRockets.add(Pair(pos.toJOML(), direction.normal.toJOMLd().mul(force * 10)))
    }

    fun removeRocket(pos: BlockPos) {
        removedRockets.add(pos.toJOML())
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): SugarRocketController {
            val attachment = ship.getAttachment(SugarRocketController::class.java)
            if (attachment == null) {
                val newAttachment = SugarRocketController()
                ship.setAttachment(newAttachment)
                return newAttachment
            } else {
                return attachment
            }
        }
    }
}