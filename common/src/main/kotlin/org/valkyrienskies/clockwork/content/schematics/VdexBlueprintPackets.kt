package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class VdexBlueprintActionPacket(val action: Action, val id: String, val position: Vec3 = Vec3.ZERO, val yaw: Float = 0f) : C2SCWPacket {
    enum class Action { DOWNLOAD, DEPLOY, PASTE, CLEAR }
    constructor(buffer: FriendlyByteBuf) : this(buffer.readEnum(Action::class.java), buffer.readUtf(64),
        Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat())
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeEnum(action); buffer.writeUtf(id, 64)
        buffer.writeDouble(position.x); buffer.writeDouble(position.y); buffer.writeDouble(position.z); buffer.writeFloat(yaw)
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            VdexBlueprintLibrary.guarded(context.sender) {
                val stack = VdexBlueprintLibrary.held(context.sender) ?: return@guarded
                if (stack.tag?.getString("VdexFile").orEmpty() != id) return@guarded
                when (action) {
                    Action.DOWNLOAD -> VdexBlueprintLibrary.download(context.sender, id)
                    Action.DEPLOY -> VdexBlueprintLibrary.deploy(context.sender, stack, position, yaw)
                    Action.PASTE -> VdexBlueprintLibrary.paste(context.sender, stack)
                    Action.CLEAR -> { stack.removeTagKey("VdexFile"); stack.removeTagKey("VdexName"); stack.removeTagKey("Deployed") }
                }
            }
        }
        context.setPacketHandled(true)
    }
}

class VdexBlueprintUploadPacket(val id: String, val name: String, val offset: Int, val total: Int, val bytes: ByteArray) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(buffer.readUtf(64), buffer.readUtf(80), buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(VdexBlueprintData.CHUNK_BYTES))
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUtf(id, 64); buffer.writeUtf(name, 80); buffer.writeVarInt(offset); buffer.writeVarInt(total); buffer.writeByteArray(bytes)
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { VdexBlueprintLibrary.upload(context.sender, this) }
        context.setPacketHandled(true)
    }
}

class VdexBlueprintDownloadPacket(val id: String, val name: String, val offset: Int, val total: Int, val bytes: ByteArray) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readUtf(64), buffer.readUtf(80), buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(VdexBlueprintData.CHUNK_BYTES))
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeUtf(id, 64); buffer.writeUtf(name, 80); buffer.writeVarInt(offset); buffer.writeVarInt(total); buffer.writeByteArray(bytes)
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { VdexBlueprintClient.accept(this) }
        context.setPacketHandled(true)
    }
}

class VdexBlueprintNoticePacket(val message: Component) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readComponent())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeComponent(message) }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { VdexBlueprintClient.notice(message) }
        context.setPacketHandled(true)
    }
}
