package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.util.MathUtil

class SmartPropSyncPacket : S2CCWPacket {

    val pos: BlockPos
    val targetQuaternion: Quaternionf

    constructor(blockPos: BlockPos, target: Quaternionf) {
        pos = blockPos
        targetQuaternion = target
    }

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        targetQuaternion = MathUtil.readQuatf(buffer)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            val level = Minecraft.getInstance().level
            if (level != null && level.getBlockEntity(pos) is SmartPropellerBearingBlockEntity) {
                val be = level.getBlockEntity(pos) as SmartPropellerBearingBlockEntity
                //be.clientTargetTiltQuat = targetQuaternion
                //be.clientTiltQuat =
            }
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        MathUtil.writeQuatf(buffer, targetQuaternion)
    }
}