package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class WandSelectionPacket : C2SCWPacket {

    val firstPos: BlockPos
    val secondPos: BlockPos
    val tool: ToolType
    val isSecond: Boolean
    val leftClick: Boolean
    val clickedFace: Int

    constructor(pos1: BlockPos, pos2: BlockPos?, tool: ToolType, leftClick: Boolean, clickedFace: Int = -1) {
        firstPos = pos1
        secondPos = pos2 ?: pos1
        this.tool = tool
        isSecond = pos2 != null
        this.leftClick = leftClick
        this.clickedFace = clickedFace
    }
    constructor(buffer: FriendlyByteBuf) {
        firstPos = buffer.readBlockPos()
        secondPos = buffer.readBlockPos()
        tool = buffer.readEnum(ToolType::class.java)
        isSecond = buffer.readBoolean()
        leftClick = buffer.readBoolean()
        clickedFace = buffer.readInt()
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val serverPlayer = context.sender
            if (serverPlayer.level() is ServerLevel) {
                val serverLevel = serverPlayer.level() as ServerLevel
                val stack = serverPlayer.mainHandItem
                val bl = stack.`is`(ClockworkItems.WANDERWAND.get().asItem())
                if (bl) {
                    //if (!serverPlayer.cooldowns.isOnCooldown(stack.item)) {
                        //serverPlayer.cooldowns.addCooldown(stack.item, 3)
                        when (tool) {
                            ToolType.SELECT, ToolType.DESELECT -> {
                                WanderwandItem.select(serverLevel, serverPlayer, firstPos, secondPos, isSecond, tool == ToolType.DESELECT, leftClick)
                            }

                            ToolType.WELD -> {
                                if (isSecond) {
                                    WanderwandItem.weld(
                                        serverLevel, serverPlayer, secondPos!!.mutable(), clickedFace
                                    )
                                } else {
                                    WanderwandItem.startWeld(
                                        serverLevel, serverPlayer, firstPos!!.mutable(), clickedFace
                                    )
                                }
                            }

                            ToolType.ATTACH -> {
                                val posToSend = if (isSecond) secondPos!!.mutable() else firstPos!!.mutable()
                                WanderwandItem.attach(
                                    serverLevel, serverPlayer, posToSend.mutable()
                                )
                            }

                            ToolType.BIND -> {
                                if (isSecond) {
                                    WanderwandItem.bind(
                                        serverLevel, serverPlayer, secondPos!!.mutable()
                                    )
                                } else {
                                    WanderwandItem.startBind(
                                        serverLevel, serverPlayer, firstPos!!.mutable()
                                    )
                                }
                            }
                        }
                    }
                //}
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(firstPos)
        buffer.writeBlockPos(secondPos)
        buffer.writeEnum(tool)
        buffer.writeBoolean(isSecond)
        buffer.writeBoolean(leftClick)
        buffer.writeInt(clickedFace)
    }
}
