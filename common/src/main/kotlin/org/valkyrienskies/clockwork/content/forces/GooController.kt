package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.ShipForcesInducer
import org.valkyrienskies.core.util.squared
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.concurrent.ConcurrentLinkedQueue

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class GooController : ShipForcesInducer {

    val collidedBlocks = ConcurrentLinkedQueue<Vector3dc>()

    fun addCollision(pos: BlockPos) {
        if (pos.toJOMLD() !in collidedBlocks) collidedBlocks.add(pos.toJOMLD())
    }

    override fun applyForces(physShip: PhysShip) {


        while (collidedBlocks.isNotEmpty()) {
            val position = physShip.transform.toModel.transformPosition(collidedBlocks.poll(), Vector3d())
            val relative = physShip.centerOfMass.sub(position, Vector3d()).normalize()

            // Apply force from goo to ship which is proportional to speed and mass of ship
            val force = relative.mul(1.45 * physShip.velocity.length() * physShip.mass / 0.05)

            physShip.applyInvariantForce(force)
        }
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): GooController? {
            if (ship.getAttachment(GooController::class.java) == null) {
                ship.setAttachment(GooController())
            }
            return ship.getAttachment(GooController::class.java)
        }
    }
}