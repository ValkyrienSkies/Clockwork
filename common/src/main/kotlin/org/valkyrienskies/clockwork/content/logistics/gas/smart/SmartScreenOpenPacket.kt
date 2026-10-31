package org.valkyrienskies.clockwork.content.logistics.gas.smart

import net.createmod.catnip.gui.ScreenOpener
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d

class SmartScreenOpenPacket(
    private val nodeA: DuctNodePos, private val nodeB: DuctNodePos,
    private val settings: SmartDuctSettings, override var player: Player? = null,
) : S2CCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        buffer.readVec3d().toDuctNodePos(buffer.readResourceLocation()),
        SmartDuctSettings.read(buffer),
    )

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { ScreenOpener.open(SmartDuctScreen(nodeA, nodeB, settings)) }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeVec3d(nodeA.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeA.dimensionId)
        buffer.writeVec3d(nodeB.toMinecraft().toVector3d())
        buffer.writeResourceLocation(nodeB.dimensionId)
        settings.write(buffer)
    }
}