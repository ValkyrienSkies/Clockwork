package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft

class UpdateGyroPacket : C2SCWPacket {
    private val pos: BlockPos
    private val targetVec3: Vector3d

    constructor(buffer: FriendlyByteBuf) {
        this.pos = buffer.readBlockPos()
        this.targetVec3 = buffer.readVec3d()
    }

    constructor(newPos: BlockPos, targetVec3: Vector3d) {
        this.pos = newPos
        this.targetVec3 = targetVec3;
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val be = context.sender.level().getBlockEntity(pos) as GyroBlockEntity?
            if (be != null && be.canPlayerUse(context.sender)) {
                be.targetVec3 = targetVec3
                be.notifyUpdate()
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeVec3d(targetVec3)
    }
}
