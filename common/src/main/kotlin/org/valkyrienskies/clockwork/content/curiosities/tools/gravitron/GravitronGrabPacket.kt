package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem.Companion.getState
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem.Companion.grabssemble
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GravitronToolBase
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class GravitronGrabPacket : C2SCWPacket {
    var clickedPos: BlockPos? = null
    var clickLocation: Vec3? = null
    var mode: Byte = 0

    constructor(buffer: FriendlyByteBuf) {
        clickedPos = buffer.readBlockPos()
        clickLocation = Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        mode = buffer.readByte()
    }

    constructor(clickedPos: BlockPos?, clickedLocation: Vec3?, mode: Byte) {
        this.clickedPos = clickedPos
        this.clickLocation = clickedLocation
        this.mode = mode
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val serverPlayer = context.sender
            if (serverPlayer.level is ServerLevel) {
                val serverLevel = serverPlayer.getLevel() as ServerLevel
                val s = getState(serverPlayer)
                val stack = serverPlayer.mainHandItem
                if (stack.`is`(ClockworkItems.GRAVITRON.get().asItem())) {
                    if ((s.shipID == null) && !serverPlayer.cooldowns
                            .isOnCooldown(stack.item) && !s.grabbing
                    ) {
                        serverPlayer.cooldowns.addCooldown(stack.item, 20)
                        s.grabCD = 20
                        if (mode == GravitronToolBase.GRAB) {
                            s.grabbing = true
                            GrabTool.tryGrabShip(serverLevel, serverPlayer, clickedPos!!.mutable(), clickLocation!!)
                        } else if (mode == GravitronToolBase.ASSEMBLE) {
                            grabssemble(
                                serverLevel, serverPlayer, clickedPos!!.mutable(),
                                clickLocation!!, false
                            )
                        } else if (mode == GravitronToolBase.GRABSSEMBLE) {
                            s.grabbing = true
                            grabssemble(
                                serverLevel, serverPlayer, clickedPos!!.mutable(),
                                clickLocation!!, true
                            )
                        }
                    }
                }
            }
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        if (clickedPos != null) {
            buffer.writeBlockPos(clickedPos!!)
        }
        buffer.writeDouble(clickLocation!!.x)
        buffer.writeDouble(clickLocation!!.y)
        buffer.writeDouble(clickLocation!!.z)
        buffer.writeByte(mode.toInt())
    }
}
