package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.client.render.goggles.AeronautGoggleOverlayRenderer
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class AeronautShipReadoutPacket(val shipId: Long, val dimension: ResourceLocation, val snapshot: AeronautShipReadout.Snapshot) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readLong(), buffer.readResourceLocation(), readSnapshot(buffer))

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeLong(shipId)
        buffer.writeResourceLocation(dimension)
        buffer.writeDouble(snapshot.mass)
        writeVector(buffer, snapshot.massCenter)
        for (center in listOf(snapshot.potentialThrust, snapshot.liveThrust, snapshot.potentialLift, snapshot.liveLift)) {
            buffer.writeBoolean(center != null)
            if (center != null) {
                writeVector(buffer, center.position)
                writeVector(buffer, center.force)
                buffer.writeDouble(center.weight)
            }
        }
        val assembly = snapshot.assembly
        buffer.writeVarInt(assembly.bodyCount)
        buffer.writeVarInt(assembly.feet.size)
        for ((anchor, load) in assembly.feet) {
            writeAnchor(buffer, anchor)
            buffer.writeDouble(load)
        }
        buffer.writeVarInt(assembly.outline.size)
        assembly.outline.forEach { writeVector(buffer, it) }
        buffer.writeVarInt(assembly.joints.size)
        for (joint in assembly.joints) {
            writeAnchor(buffer, joint.anchor)
            buffer.writeEnum(joint.kind)
            buffer.writeDouble(joint.measured)
            writeOptionalNumber(buffer, joint.target)
            writeOptionalNumber(buffer, joint.lower)
            writeOptionalNumber(buffer, joint.upper)
            writeOptionalNumber(buffer, joint.error)
        }
        buffer.writeVarInt(assembly.contactFaces.size)
        for ((body, center, load) in assembly.contactFaces) {
            buffer.writeLong(body ?: -1L)
            writeVector(buffer, center)
            buffer.writeDouble(load)
        }
        buffer.writeByte(snapshot.visibleGuides or if (snapshot.massVisible) MASS_VISIBLE else 0)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { AeronautGoggleOverlayRenderer.accept(this) }
        context.setPacketHandled(true)
    }

    companion object {
        private const val MASS_VISIBLE = 8

        private fun readSnapshot(buffer: FriendlyByteBuf): AeronautShipReadout.Snapshot {
            val mass = buffer.readDouble()
            val center = readVector(buffer)
            val potentialThrust = readCenter(buffer)
            val liveThrust = readCenter(buffer)
            val potentialLift = readCenter(buffer)
            val liveLift = readCenter(buffer)
            val assembly = readAssembly(buffer)
            val visibility = if (buffer.isReadable) buffer.readUnsignedByte().toInt()
                else MASS_VISIBLE or AeronautShipReadout.ALL_GUIDES
            return AeronautShipReadout.Snapshot(mass, center, potentialThrust, liveThrust, potentialLift, liveLift, assembly,
                visibility and MASS_VISIBLE != 0, visibility and AeronautShipReadout.ALL_GUIDES)
        }

        private fun writeVector(buffer: FriendlyByteBuf, vector: Vector3dc) {
            buffer.writeDouble(vector.x()); buffer.writeDouble(vector.y()); buffer.writeDouble(vector.z())
        }
        private fun readVector(buffer: FriendlyByteBuf) = Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        private fun readCenter(buffer: FriendlyByteBuf): AeronautForceCenter? =
            if (buffer.readBoolean()) AeronautForceCenter(readVector(buffer), readVector(buffer), buffer.readDouble()) else null

        private fun writeAnchor(buffer: FriendlyByteBuf, anchor: AeronautAssemblyReadout.Anchor) {
            buffer.writeLong(anchor.shipId ?: -1L)
            buffer.writeBlockPos(anchor.pos)
        }

        private fun readAnchor(buffer: FriendlyByteBuf) =
            AeronautAssemblyReadout.Anchor(buffer.readLong().takeUnless { it == -1L }, buffer.readBlockPos())

        private fun writeOptionalNumber(buffer: FriendlyByteBuf, value: Double?) {
            buffer.writeBoolean(value != null)
            if (value != null) buffer.writeDouble(value)
        }

        private fun readOptionalNumber(buffer: FriendlyByteBuf): Double? =
            if (buffer.readBoolean()) buffer.readDouble() else null

        private fun readAssembly(buffer: FriendlyByteBuf): AeronautAssemblyReadout.Detail {
            if (!buffer.isReadable) return AeronautAssemblyReadout.Detail()
            val count = buffer.readVarInt()
            require(count > 0)
            val feet = readList(buffer, AeronautAssemblyReadout.MAX_MARKERS) {
                AeronautAssemblyReadout.Foot(readAnchor(buffer), buffer.readDouble())
            }
            val outline = readList(buffer, AeronautAssemblyReadout.MAX_OUTLINE_POINTS) { readVector(buffer) }
            val joints = readList(buffer, AeronautAssemblyReadout.MAX_MARKERS) {
                AeronautAssemblyReadout.Joint(readAnchor(buffer), buffer.readEnum(AeronautAssemblyReadout.JointKind::class.java),
                    buffer.readDouble(), readOptionalNumber(buffer), readOptionalNumber(buffer), readOptionalNumber(buffer), readOptionalNumber(buffer))
            }
            val contacts = if (buffer.isReadable) readList(buffer, AeronautAssemblyReadout.MAX_CONTACT_FACES) {
                AeronautAssemblyReadout.ContactFace(buffer.readLong().takeUnless { it == -1L }, readVector(buffer), buffer.readDouble())
            } else emptyList()
            return AeronautAssemblyReadout.Detail(count, feet, outline, joints, contacts)
        }

        private inline fun <T> readList(buffer: FriendlyByteBuf, limit: Int, read: () -> T): List<T> {
            val size = buffer.readVarInt()
            require(size in 0..limit)
            return List(size) { read() }
        }
    }
}
