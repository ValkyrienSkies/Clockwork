package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.clockwork.content.contraptions.smart_propeller.SmartPropData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class SmartPropellerController : ShipForcesInducer {

    private val propellorPhysData: HashMap<Int, PropData> = HashMap<Int, PropData>()

    private val propellorUpdatePhysData: ConcurrentHashMap<Int, PropUpdateData> =
        ConcurrentHashMap<Int, PropUpdateData>()

    private val createdProps: ConcurrentLinkedQueue<Pair<Int, SmartPropData>> =
        ConcurrentLinkedQueue<Pair<Int, SmartPropData>>()
    private val removedProps = ConcurrentLinkedQueue<Int>()
    private var nextPropID = 0

    override fun applyForces(physShip: PhysShip) {

        while (!removedProps.isEmpty()) {
            propellorPhysData.remove(removedProps.remove() as Int)
        }


    }

    fun addPropeller(data: SmartPropData): Int {
        val id = nextPropID++
        createdProps.add(Pair<Int, SmartPropData>(id, data))
        return id
    }

    fun removePropeller(id: Int) {
        removedProps.add(id)
    }

    fun updatePropeller(id: Int, data: PropUpdateData) {
        propellorUpdatePhysData[id] = data
    }

    override fun equals(other: Any?): Boolean {
        // self check
        return if (this === other) {
            true
        } else if (other !is SmartPropellerController) {
            false
        } else {
            (propellorPhysData == other.propellorPhysData && propellorUpdatePhysData == other.propellorUpdatePhysData && areQueuesEqual<Pair<Int, SmartPropData>>(
                createdProps,
                other.createdProps
            ) && areQueuesEqual<Int>(removedProps, other.removedProps) && nextPropID == other.nextPropID)
        }
    }

    companion object {
        fun getOrCreate(ship: ServerShip): SmartPropellerController? {
            if (ship.getAttachment(SmartPropellerController::class.java) == null) {
                ship.saveAttachment(SmartPropellerController::class.java, SmartPropellerController())
            }
            return ship.getAttachment(SmartPropellerController::class.java)
        }

        inline fun <reified T> areQueuesEqual(left: Queue<T>, right: Queue<T>): Boolean {
            return left.toTypedArray().contentEquals(right.toTypedArray())
        }
    }
}