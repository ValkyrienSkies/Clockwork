package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.CreativeGravitronItem.Companion.grabssemble
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
                val stack = serverPlayer.mainHandItem
                val bl = stack.`is`(ClockworkItems.CREATIVE_GRAVITRON.get().asItem())
                val bl2 = stack.`is`(ClockworkItems.GRAVITRON.get().asItem())
                if (bl2 || bl) {
                    if (!serverPlayer.cooldowns.isOnCooldown(stack.item)) {
                        serverPlayer.cooldowns.addCooldown(stack.item, 20)
                        if (bl2) {
                            mode = GravitronToolBase.GRAB
                        }
                        when (mode) {
                            GravitronToolBase.GRAB -> {
                                GrabTool.tryGrabShip(serverLevel, serverPlayer, clickedPos!!.mutable(), clickLocation!!, bl)
                            }

                            GravitronToolBase.ASSEMBLE -> {
                                grabssemble(
                                    serverLevel, serverPlayer, clickedPos!!.mutable(),
                                    clickLocation!!, false
                                )
                            }

                            GravitronToolBase.GRABSSEMBLE -> {
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
    }

    override fun write(buffer: FriendlyByteBuf) {
        if (clickedPos != null) {
            buffer.writeBlockPos(clickedPos!!)
        }
        if (clickLocation != null) {
            buffer.writeDouble(clickLocation!!.x)
            buffer.writeDouble(clickLocation!!.y)
            buffer.writeDouble(clickLocation!!.z)
        }

        buffer.writeByte(mode.toInt())
    }
}
