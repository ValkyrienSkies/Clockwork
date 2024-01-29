package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.clockwork.util.ShipDestroyer.unfillShip
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor

class GravitronDestroyPacket : C2SCWPacket {
    var clickedPos: BlockPos? = null

    constructor(buffer: FriendlyByteBuf) {
        clickedPos = buffer.readBlockPos()
    }

    constructor(clickedPos: BlockPos?) {
        this.clickedPos = clickedPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val serverPlayer = context.sender
            val serverLevel = serverPlayer.getLevel()

            val ship = serverLevel.getShipManagingPos(clickedPos!!)
            if (ship != null) {
                val invRotation = ship.transform.shipToWorldRotation.invert(Quaterniond())
                val invRotationAxisAngle = AxisAngle4d(invRotation)

                val f = floor((invRotationAxisAngle.angle / (PI * 0.5)) + 4.5).toInt() % 4
                var alignTarget = Direction.from2DDataValue(f)
                if (invRotation.y < 0 && alignTarget != Direction.NORTH) {
                    alignTarget = alignTarget.opposite
                }
                unfillShip(serverLevel, shipe, alignTarget)
            }
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(clickedPos!!)
    }
}
