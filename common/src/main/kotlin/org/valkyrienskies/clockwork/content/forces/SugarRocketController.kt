package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.concurrent.ConcurrentLinkedQueue

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class SugarRocketController : ShipForcesInducer {

    val newRockets = ConcurrentLinkedQueue<Pair<Vector3i, Vector3d>>()
    val removedRockets = ConcurrentLinkedQueue<Vector3i>()
    val burningRockets: HashSet<SugarRocketData> = HashSet() // Used instead of HashMap for auto Serialization

    override fun applyForces(physShip: PhysShip) {
        while (newRockets.isNotEmpty()) {
            val rocket = newRockets.poll()
            burningRockets.add(SugarRocketData(rocket.first, rocket.second))
            //println("added rocket at ${rocket.first} with force ${rocket.second}")
        }
        while (removedRockets.isNotEmpty()) {
            val rocket = removedRockets.poll()
            burningRockets.removeIf {data -> data.position == rocket}
            //println("removed rocket at $rocket")
        }
        burningRockets.forEach { data ->
            val shipPos = Vector3d(data.position).add(0.5, 0.5, 0.5, Vector3d()).sub(physShip.transform.positionInShip)
            physShip.applyRotDependentForceToPos(data.force!!, shipPos)
        }
    }

    fun addRocket(pos: BlockPos, force: Double, direction: Direction) {
        newRockets.add(Pair(pos.toJOML(), direction.normal.toJOMLD().mul(force * 10)))
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