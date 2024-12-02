package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.contraptions.phys.gas_thruster.GasThrusterData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.mod.common.util.toJOMLD

@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY,)
class GasThrusterController : ShipForcesInducer {
    val thrusterData = Int2ObjectOpenHashMap<GasThrusterData>()

    override fun applyForces(physShip: PhysShip) {
       for (thruster in thrusterData.values) {
           if (thruster.position == null || thruster.force == null || thruster.force.length() == 0.0) continue
           val pos =  thruster.position.add(0.5,0.5,0.5, Vector3d()).sub(physShip.transform.positionInShip)
           val force = physShip.transform.worldToShip.transformDirection(Vector3d(thruster.force))


           physShip.applyRotDependentForceToPos(force, pos)
       }
    }

    fun updateThruster(thrusterPos: BlockPos, force: Vector3d) {
        thrusterData.put(thrusterPos.hashCode(), GasThrusterData(thrusterPos.toJOMLD(), force))
    }

    fun deleteThruster(thrusterPos: BlockPos) {

        thrusterData.remove(thrusterPos.hashCode())
    }

    companion object {
        fun getOrCreate(ship: ServerShip): GasThrusterController? {
            if (ship.getAttachment(GasThrusterController::class.java) == null) {
                ship.saveAttachment(GasThrusterController::class.java, GasThrusterController())
            }
            return ship.getAttachment(GasThrusterController::class.java)
        }
    }
}