package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.mod.common.toWorldCoordinates

class ConfigureCombobulatorPacket(
    private val pos: BlockPos,
    private val revision: Int,
    private val program: CombobulatorProgram,
) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readBlockPos(), buffer.readVarInt(), CombobulatorProgram.read(requireNotNull(buffer.readNbt())),
    )

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeVarInt(revision)
        buffer.writeNbt(program.write())
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            val level = player.level()
            if (!player.mayBuild() || !level.isLoaded(pos) || !level.mayInteract(player, pos)) return@enqueueWork
            if (player.distanceToSqr(level.toWorldCoordinates(Vec3.atCenterOf(pos))) > 64) return@enqueueWork
            (level.getBlockEntity(pos) as? CombobulatorBlockEntity)?.updateProgram(program, revision)
        }
        context.setPacketHandled(true)
    }
}
