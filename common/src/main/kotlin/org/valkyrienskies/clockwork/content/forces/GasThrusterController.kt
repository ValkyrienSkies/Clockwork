package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data.GasThrusterCreateData
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data.GasThrusterData
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data.GasThrusterUpdateData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.setAttachment
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.concurrent.ConcurrentLinkedQueue

@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY,)
class GasThrusterController(
    override val appliers: HashMap<Int, GasThrusterData> = HashMap(),
    override val applierUpdateData: ConcurrentLinkedQueue<Pair<Int, GasThrusterUpdateData>> = ConcurrentLinkedQueue(),
    override val createdAppliers: ConcurrentLinkedQueue<Pair<Int, GasThrusterCreateData>> = ConcurrentLinkedQueue(),
    override val removedAppliers: ConcurrentLinkedQueue<Int> = ConcurrentLinkedQueue(),
    override var nextApplierID: Int = 0
) : MultiInstanceForceApplier<GasThrusterUpdateData, GasThrusterData, GasThrusterCreateData> {

    override fun applyForces(physShip: PhysShip) {
        super.applyForces(physShip)
       for (thruster in appliers.values) {
           if (thruster.position == null || thruster.force == null || thruster.force!!.length() == 0.0) continue
           val pos =  Vector3d(thruster.position).add(0.5,0.5,0.5, Vector3d()).sub(physShip.transform.positionInShip)
           val force = thruster.force!!.mul(2.0, Vector3d())


           physShip.applyRotDependentForceToPos(force!!, pos)
       }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): GasThrusterController? {
            if (ship.getAttachment(GasThrusterController::class.java) == null) {
                ship.setAttachment(GasThrusterController())
            }
            return ship.getAttachment(GasThrusterController::class.java)
        }
    }
}