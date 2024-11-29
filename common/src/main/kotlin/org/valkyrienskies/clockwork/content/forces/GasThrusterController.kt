package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.mod.common.util.toJOMLD

@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
class GasThrusterController : ShipForcesInducer {
    @JsonIgnore
    val ThrusterData = HashMap<BlockPos, Vector3d>()

    override fun applyForces(physShip: PhysShip) {
       for (thruster in ThrusterData) {
           if (thruster.value.length() == 0.0) continue
           val pos =  thruster.key.toJOMLD().add(0.5,0.5,0.5).sub(physShip.transform.positionInShip)
           val force = physShip.transform.worldToShip.transformDirection(thruster.value)


           physShip.applyRotDependentForceToPos(force, pos)
       }
    }

    fun updateThruster(thrusterPos: BlockPos, force: Vector3d) {
        ThrusterData[thrusterPos] = force
    }

    fun deleteThruster(thrusterPos: BlockPos) {
        ThrusterData.remove(thrusterPos)
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