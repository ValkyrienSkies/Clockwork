package org.valkyrienskies.clockwork.content.forces

import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierUpdateData
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

interface MultiInstanceForceApplier<A: ForceApplierUpdateData, D: ForceApplierData<A>, C: ForceApplierCreateData<D>>: ShipForcesInducer {
    val appliers: HashMap<Int, D>
    val applierUpdateData: ConcurrentLinkedQueue<Pair<Int, A>>
    val createdAppliers: ConcurrentLinkedQueue<Pair<Int, C>>
    val removedAppliers: ConcurrentLinkedQueue<Int>
    var nextApplierID: Int

    override fun applyForces(physShip: PhysShip) {
        pollChanges()
    }

    fun createApplier(createData: C): Int {
        val id = nextApplierID
        nextApplierID++
        createdAppliers.add(id to createData)
        return id
    }

    fun removeApplier(id: Int) {
        removedAppliers.add(id)
    }

    fun updateApplier(id: Int, updateData: A) {
        applierUpdateData.add(id to updateData)
    }

    fun pollChanges() {
        while (createdAppliers.isNotEmpty()) {
            val createData = createdAppliers.poll()
            appliers[createData.first] = createData.second.fromCreateData()
        }
        while (applierUpdateData.isNotEmpty()) {
            val updateData = applierUpdateData.poll()
            appliers[updateData.first]?.updateData(updateData.second)
        }
        while (removedAppliers.isNotEmpty()) {
            appliers.remove(removedAppliers.poll())
        }
    }

    companion object {
        fun getOrCreate(ship: ServerShip): MultiInstanceForceApplier<*, *, *>? {
            throw IllegalArgumentException("Invalid data type")
        }
    }
}