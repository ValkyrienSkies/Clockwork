package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import java.util.UUID

class GatemakyrEntityTransitPacket(private val dimension: ResourceLocation, private val id: Int, private val uuid: UUID,
                                  private val contact: Vec3, private val feet: Vec3, private val velocity: Vec3,
                                  private val yaw: Float, private val pitch: Float) : S2CCWPacket {
    override var player: Player? = null

    constructor(buf: FriendlyByteBuf) : this(buf.readResourceLocation(), buf.readVarInt(), buf.readUUID(),
        buf.vector(), buf.vector(), buf.vector(), buf.readFloat(), buf.readFloat())

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(dimension)
        buffer.writeVarInt(id)
        buffer.writeUUID(uuid)
        buffer.vector(contact)
        buffer.vector(feet)
        buffer.vector(velocity)
        buffer.writeFloat(yaw)
        buffer.writeFloat(pitch)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            val mc = Minecraft.getInstance()
            val level = mc.level ?: return@enqueueWork
            if (level.dimension().location() != dimension) return@enqueueWork
            val entity = level.getEntity(id) ?: return@enqueueWork
            if (entity.uuid != uuid || entity === mc.player) return@enqueueWork
            entity.moveTo(feet.x, feet.y, feet.z, yaw, pitch)
            entity.lerpTo(feet.x, feet.y, feet.z, yaw, pitch, 0, true)
            entity.xOld = contact.x
            entity.yOld = contact.y
            entity.zOld = contact.z
            entity.xo = contact.x
            entity.yo = contact.y
            entity.zo = contact.z
            if (entity is LivingEntity) {
                entity.lerpHeadTo(yaw, 0)
                entity.yHeadRot = yaw
                entity.yHeadRotO = yaw
                entity.yBodyRot = yaw
                entity.yBodyRotO = yaw
            }
            entity.deltaMovement = velocity
            entity.setOnGround(false)
            entity.fallDistance = 0f
        }
        context.setPacketHandled(true)
    }

    companion object {
        internal fun send(entity: Entity, contact: Vec3) {
            ClockworkPackets.sendToClientsTracking(GatemakyrEntityTransitPacket(entity.level().dimension().location(),
                entity.id, entity.uuid, contact, entity.position(), entity.deltaMovement, entity.yRot, entity.xRot), entity)
        }

        private fun FriendlyByteBuf.vector() = Vec3(readDouble(), readDouble(), readDouble())
        private fun FriendlyByteBuf.vector(v: Vec3) {
            writeDouble(v.x)
            writeDouble(v.y)
            writeDouble(v.z)
        }
    }
}
