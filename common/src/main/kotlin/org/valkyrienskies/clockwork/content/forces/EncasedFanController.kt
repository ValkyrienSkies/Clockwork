package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import it.unimi.dsi.fastutil.Pair
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.util.Mth
import net.minecraft.world.entity.animal.horse.Horse
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanCreateData
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanData
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanUpdateData
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiConsumer

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class EncasedFanController : ShipForcesInducer {
    val fanData: Int2ObjectOpenHashMap<EncasedFanData> = Int2ObjectOpenHashMap<EncasedFanData>()
    private val fanUpdateData: ConcurrentHashMap<Int, EncasedFanUpdateData> =
        ConcurrentHashMap<Int, EncasedFanUpdateData>()
    private val createdFans: ConcurrentLinkedQueue<Pair<Int, EncasedFanCreateData>> =
        ConcurrentLinkedQueue<Pair<Int, EncasedFanCreateData>>()
    private val removedFans = ConcurrentLinkedQueue<Int>()
    private var nextFanID = 0
    override fun applyForces(physShip: PhysShip) {
        while (!createdFans.isEmpty()) {
            val createData: Pair<Int, EncasedFanCreateData> = createdFans.remove()
            fanData.put(
                createData.left(), EncasedFanData(
                    createData.right().fanPos,
                    createData.right().fanDir,
                    createData.right().fanSpeed
                )
            )
        }
        while (!removedFans.isEmpty()) {
            fanData.remove(removedFans.remove() as Int)
        }
        fanUpdateData.forEach(BiConsumer<Int, EncasedFanUpdateData> forEach@{ id: Int?, data: EncasedFanUpdateData ->
            val physData: EncasedFanData = fanData[id] ?: return@forEach
            physData.fanSpeed = data.fanSpeed
        })
        fanUpdateData.clear()
        for (physData in fanData.values) {
            val force = computeForce(physData, physShip)
            val fanVector: Vector3dc =
                physData.fanPos.add(0.5, 0.5, 0.5, Vector3d()).sub(physShip.transform.positionInShip)
            physShip.applyRotDependentForceToPos(force, fanVector)
        }
    }

    private fun computeForce(
        physData: EncasedFanData,
        physShip: PhysShip
    ): Vector3dc {
        val speed: Double = physData.fanSpeed
        val dir: Vector3d = Vector3d(physData.fanDir).mul(Math.signum(speed))
        var providedForce = Math.abs(speed) * 36.00875
        val airPress = airPressure(physShip.transform.positionInWorld)
        val fanPosRelCenterMass: Vector3dc = physShip.transform.shipToWorld.transformPosition(
            physData.fanPos.add(0.5, 0.5, 0.5, Vector3d()),
            Vector3d()
        ).sub(physShip.transform.positionInWorld, Vector3d())
        val worldVelAtFan: Vector3dc = physShip.omega.cross(fanPosRelCenterMass, Vector3d())
            .add(physShip.velocity, Vector3d())
        val exhaustVel = exhaustVelocity()
        var factor = 1.0 - Mth.clamp(dir.dot(worldVelAtFan) / exhaustVel, 0.0, 1.0)
        if (!java.lang.Double.isFinite(factor)) {
            factor = 0.0
        }

        providedForce = providedForce * airPress * factor
        return dir.mul(providedForce)
    }

    private fun airPressure(pos: Vector3dc): Double {
        val SEA_LEVEL = 64.0
        val WORLD_HEIGHT = 320.0
        val FALLOFF_POINT = 192.0
        val offset = Math.exp(-(WORLD_HEIGHT - SEA_LEVEL) / FALLOFF_POINT)
        val height = pos.y()
        val airPress = (Math.exp(-(height - SEA_LEVEL) / FALLOFF_POINT) - offset) / (1.0 - offset)
        return if (java.lang.Double.isFinite(airPress)) {
            Mth.clamp(airPress, 0.0, 1.0)
        } else {
            0.0
        }
    }

    private fun exhaustVelocity(): Double {
        return 44.074
    }

    fun addEncasedFan(data: EncasedFanCreateData): Int {
        val id = nextFanID++
        createdFans.add(Pair.of<Int, EncasedFanCreateData>(id, data))
        return id
    }

    fun removeEncasedFan(id: Int) {
        removedFans.add(id)
    }

    fun updateEncasedFan(id: Int, data: EncasedFanUpdateData) {
        fanUpdateData[id] = data
    }

    companion object {
        fun getOrCreate(ship: ServerShip): EncasedFanController? {
            if (ship.getAttachment(EncasedFanController::class.java) == null) {
                ship.saveAttachment(EncasedFanController::class.java, EncasedFanController())
            }
            return ship.getAttachment(EncasedFanController::class.java)
        }
    }
}