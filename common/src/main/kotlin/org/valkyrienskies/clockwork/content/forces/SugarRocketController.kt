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

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class SugarRocketController : ShipForcesInducer {
    val burningRockets: HashMap<Vector3i, Vector3d> = HashMap()

    override fun applyForces(physShip: PhysShip) {
        burningRockets.forEach { (pos, force) ->
            val shipPos = Vector3d(pos).add(0.5, 0.5, 0.5, Vector3d()).sub(physShip.transform.positionInShip)
            physShip.applyRotDependentForceToPos(force, shipPos)
        }
    }

    fun addRocket(pos: BlockPos, force: Double, direction: Direction) {
        burningRockets[pos.toJOML()] = direction.opposite.normal.toJOMLd().mul(force, Vector3d())
    }

    fun removeRocket(pos: BlockPos) {
        burningRockets.remove(pos.toJOML())
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): SugarRocketController {
            return ship.getAttachment(SugarRocketController::class.java) ?: ship.setAttachment(SugarRocketController())!!
        }
    }
}