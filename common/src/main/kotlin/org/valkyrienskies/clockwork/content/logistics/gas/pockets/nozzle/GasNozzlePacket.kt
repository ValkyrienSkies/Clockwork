package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor.AirCompressorBlockEntity
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class GasNozzlePacket: S2CCWPacket {
    override var player: Player? = null


    private val chaseTarget: Double
    private val pos: BlockPos


    constructor(buffer: FriendlyByteBuf) {
        chaseTarget = buffer.readDouble()
        pos = buffer.readBlockPos()

    }

    constructor(newChaseTarget: Double, newPos: BlockPos) {
        chaseTarget = newChaseTarget
        pos = newPos
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {

            val be =
                Minecraft.getInstance().level?.getBlockEntity(pos) as GasNozzleBlockEntity? ?: return@enqueueWork
            be.pointer.chase(chaseTarget, be.getChaseSpeed().toDouble(), LerpedFloat.Chaser.LINEAR)

        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeDouble(chaseTarget)
        buffer.writeBlockPos(pos)

    }
}