package org.valkyrienskies.clockwork.content.forces

import it.unimi.dsi.fastutil.Pair
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelCreateData
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelData
import org.valkyrienskies.clockwork.content.physicalities.reaction_wheel.ReactionWheelUpdateData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.impl.api.ShipForcesInducer
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiConsumer

class ReactionWheelController : ShipForcesInducer {
    val reactionwheelData: Int2ObjectOpenHashMap<ReactionWheelData> = Int2ObjectOpenHashMap<ReactionWheelData>()
    private val reactionwheelUpdateData: ConcurrentHashMap<Int, ReactionWheelUpdateData> =
        ConcurrentHashMap<Int, ReactionWheelUpdateData>()
    private val createdRWs: ConcurrentLinkedQueue<Pair<Int, ReactionWheelCreateData>> =
        ConcurrentLinkedQueue<Pair<Int, ReactionWheelCreateData>>()
    private val removedRWs = ConcurrentLinkedQueue<Int>()
    private var nextRWID = 0
    override fun applyForces(physShip: PhysShip) {
        while (!createdRWs.isEmpty()) {
            val createData: Pair<Int, ReactionWheelCreateData> = createdRWs.remove()
            reactionwheelData.put(
                createData.left(), ReactionWheelData(
                    createData.right().wheelPos(),
                    createData.right().wheelAxis(),
                    createData.right().wheelSpeed(),
                    createData.right().spinup(),
                    createData.right().spindown(),
                    createData.right().active(),
                    createData.right().sourceSpeed()
                )
            )
        }
        while (!removedRWs.isEmpty()) {
            reactionwheelData.remove(removedRWs.remove() as Int)
        }
        reactionwheelUpdateData.forEach(BiConsumer<Int, ReactionWheelUpdateData> { id: Int?, data: ReactionWheelUpdateData ->
            val physData: ReactionWheelData = reactionwheelData[id] ?: return@forEach
            physData.wheelSpeed = data.speed()
            physData.sourceSpeed = data.sourceSpeed()
        })
        reactionwheelUpdateData.clear()
        for (physData in reactionwheelData.values) {
//            if (physData.wheelAxis.x() == 1) {
//                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().x())) >= 10;
//            } else if (physData.wheelAxis.y() == 1) {
//                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().y())) >= 10;
//            } else if (physData.wheelAxis.z() == 1) {
//                physData.active = Math.abs((((PhysShipImpl) physShip).getPoseVel().getOmega().z())) >= 10;
//            }
            //FOR TESTING
            physData.active = true
            if (physData.sourceSpeed !== 0) {
                if (physData.active) {
                    val torque = computeTorque(
                        physShip.transform, physData, (physShip as PhysShipImpl).poseVel.omega,
                        physShip
                    )
                    //                    Vector3dc troque = computeResistance(((PhysShipImpl) physShip), physData);
                    if (torque.isFinite) {
                        physShip.applyInvariantTorque(torque)
                    }
                }
            }
        }
    }

    private fun computeTorque(
        physTransform: ShipTransform,
        physWheel: ReactionWheelData,
        omega: Vector3dc,
        physShip: PhysShipImpl
    ): Vector3dc {
        val prevAngMomentumRelWheel: Vector3dc = physWheel.prevAngMomentum
        val wheelPos: Vector3dc = physWheel.wheelPos
        val wheelAxis: Vector3dc = Vector3d(physWheel.wheelAxis)
        val wheelSpeed: Double = physWheel.wheelSpeed
        val wheelMass = 18000.0

        // 1/2 * Mass * (Outer Wheel Radius^2 + Total Wheel Radius^2)
        val wheelInertia = 0.5 * wheelMass * (Math.pow(0.25, 2.0) + Math.pow(0.75, 2.0))
        val rotVel = wheelSpeed * (2 * Math.PI / 60)
        val r: Vector3dc =
            Vector3d(wheelPos).sub(physTransform.positionInShip).rotate(physTransform.shipToWorldRotation)
        val angularMomentumRelWheel: Vector3dc = Vector3d(wheelAxis).mul(rotVel).mul(wheelInertia)

        // Add to convert from momentum relative to wheel into relative to ship
        val momentumModifier: Vector3dc = Vector3d(omega).cross(r).mul(wheelMass)
        val angularMomentumRelShip: Vector3dc = Vector3d(angularMomentumRelWheel).add(momentumModifier)
        val prevAngularMomentumRelShip: Vector3dc = Vector3d(prevAngMomentumRelWheel).add(momentumModifier)
        val torque: Vector3dc = Vector3d(prevAngularMomentumRelShip).sub(angularMomentumRelShip).div(1 / 60.0)
        physWheel.prevAngMomentum = angularMomentumRelWheel
        return torque.mul(10.0, Vector3d())
    }

    fun addReactionWheel(data: ReactionWheelCreateData): Int {
        val id = nextRWID++
        createdRWs.add(Pair.of<Int, ReactionWheelCreateData>(id, data))
        return id
    }

    fun removeReactionWheel(id: Int) {
        removedRWs.add(id)
    }

    fun updateReactionWheel(id: Int, data: ReactionWheelUpdateData) {
        reactionwheelUpdateData[id] = data
    }

    fun checkReactionWheel(id: Int?): Boolean {
        return if (id != null) {
            reactionwheelData.containsKey(id)
        } else false
    }

    companion object {
        fun getOrCreate(ship: ServerShip): ReactionWheelController? {
            if (ship.getAttachment(ReactionWheelController::class.java) == null) {
                ship.saveAttachment(ReactionWheelController::class.java, ReactionWheelController())
            }
            return ship.getAttachment(ReactionWheelController::class.java)
        }
    }
}