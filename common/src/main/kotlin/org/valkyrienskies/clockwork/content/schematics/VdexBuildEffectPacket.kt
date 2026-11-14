package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getShipManagingPos
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import kotlin.math.PI
import kotlin.math.sin

class VdexBuildEffectPacket(val start: Vec3, val end: Vec3, val other: Vec3, val item: ItemStack, val duration: Int, val connection: Boolean) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(readPoint(buffer), readPoint(buffer), readPoint(buffer), buffer.readItem(), buffer.readVarInt().coerceIn(1, 60), buffer.readBoolean())
    override fun write(buffer: FriendlyByteBuf) {
        for (point in listOf(start, end, other)) { buffer.writeDouble(point.x); buffer.writeDouble(point.y); buffer.writeDouble(point.z) }
        buffer.writeItem(item); buffer.writeVarInt(duration); buffer.writeBoolean(connection)
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { VdexBuildEffects.accept(this) }
        context.setPacketHandled(true)
    }
    companion object {
        fun cannonPosition(level: ServerLevel, pos: BlockPos): Vec3 {
            val ship = level.getShipManagingPos(pos) ?: return Vec3.atCenterOf(pos)
            val point = ship.shipToWorld.transformPosition(Vector3d(pos.x + .5, pos.y + .5, pos.z + .5))
            return Vec3(point.x, point.y, point.z)
        }
        private fun readPoint(buffer: FriendlyByteBuf) = Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        fun send(level: ServerLevel, cannon: BlockPos, a: Vec3, b: Vec3, item: ItemStack, duration: Int, connection: Boolean) {
            val origin = cannonPosition(level, cannon).add(0.0, .6, 0.0)
            ClockworkPackets.sendToNear(level, BlockPos.containing(origin), 320, VdexBuildEffectPacket(origin, a, b, item.copy(), duration, connection))
        }
    }
}

object VdexBuildEffects {
    private data class Flight(val packet: VdexBuildEffectPacket, val start: Long)
    private val flights = ArrayList<Flight>()
    private var lastLevel: Any? = null
    fun accept(packet: VdexBuildEffectPacket) {
        val level = Minecraft.getInstance().level ?: return
        if (lastLevel !== level) { lastLevel = level; flights.clear() }
        if (flights.size < 2048) flights.add(Flight(packet, level.gameTime))
    }

    fun render(pose: PoseStack, buffers: MultiBufferSource, camera: Vec3, partial: Float) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        if (lastLevel !== level) { flights.clear(); return }
        flights.removeIf { level.gameTime - it.start > it.packet.duration }
        for (flight in flights) {
            val packet = flight.packet
            val t = ((level.gameTime - flight.start + partial) / packet.duration).coerceIn(0f, 1f).toDouble()
            pose.pushPose()
            if (packet.connection) {
                pose.translate(-camera.x, -camera.y, -camera.z)
                val consumer = buffers.getBuffer(RenderType.lines())
                val direction = packet.other.subtract(packet.end).normalize()
                val side = direction.cross(Vec3(0.1, 1.0, 0.0)).normalize()
                fun point(u: Double) = packet.end.lerp(packet.other, u).add(side.scale(sin(u * PI * 8 - t * PI * 3) * sin(u * PI) * .2 * (1 - t)))
                val alpha = ((1 - t) * 220).toInt()
                for (i in 0 until 32) {
                    val a = point(i / 32.0); val b = point((i + 1) / 32.0)
                    val n = b.subtract(a).normalize()
                    for (p in listOf(a, b)) consumer.vertex(pose.last().pose(), p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                        .color(220, 232, 239, alpha).normal(pose.last().normal(), n.x.toFloat(), n.y.toFloat(), n.z.toFloat()).endVertex()
                }
            } else {
                val point = packet.start.lerp(packet.end, t).add(0.0, sin(t * PI) * (1 + packet.start.distanceTo(packet.end) * .12), 0.0)
                pose.translate(point.x - camera.x, point.y - camera.y, point.z - camera.z)
                pose.mulPose(Quaternionf().rotationXYZ((t * PI * 2).toFloat(), (t * PI).toFloat(), 0f))
                pose.scale(.65f, .65f, .65f)
                mc.itemRenderer.renderStatic(packet.item, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0)
            }
            pose.popPose()
        }
    }
}
