package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class PhysicsInfuserSyncPacket : S2CCWPacket {
    override var player: Player? = null
    private val state: CompoundTag
    private val pos: BlockPos

    constructor(buf: FriendlyByteBuf) {
        pos = buf.readBlockPos()
        state = buf.readNbt() ?: CompoundTag()
    }

    constructor(tile: PhysicsInfuserBlockEntity) {
        state = CompoundTag().also { tile.write(it, true) }
        pos = tile.blockPos
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeNbt(state)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            val level = Minecraft.getInstance().level ?: return@enqueueWork
            val blockEntity = level.getBlockEntity(pos) as? PhysicsInfuserBlockEntity ?: return@enqueueWork
            blockEntity.read(state, true)
            InfuserEffects.track(blockEntity)
        }
        context.setPacketHandled(true)
    }
}
