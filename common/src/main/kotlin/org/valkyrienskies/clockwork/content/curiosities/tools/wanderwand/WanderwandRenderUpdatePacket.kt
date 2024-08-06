package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.api.ships.properties.ShipId

class WanderwandRenderUpdatePacket : S2CCWPacket {

    val selectionPos: BlockPos
    val tool: ToolType
    val blocks: CompoundTag?
    val secondPos: BlockPos?
    val selectionDir: Direction?
    val secondDir: Direction?
    val shipId: ShipId?
    val onOff: Boolean

    constructor(selPos: BlockPos, tool: ToolType, pos2: BlockPos? = null, blocks: CompoundTag? = null, selDir: Direction? = null, dir2: Direction? = null, shipId: ShipId? = null, onOff: Boolean = false) {
        this.selectionPos = selPos
        this.tool = tool
        this.blocks = blocks
        this.secondPos = pos2
        this.selectionDir = selDir
        this.secondDir = dir2
        this.shipId = shipId
        this.onOff = onOff
    }
    constructor(buffer: FriendlyByteBuf) {
        selectionPos = buffer.readBlockPos()
        tool = buffer.readEnum(ToolType::class.java)
        if (tool == ToolType.SELECT || tool == ToolType.DESELECT) {
            blocks = buffer.readNbt()
            secondPos = null
            selectionDir = null
            secondDir = null
            shipId = null
        } else if (tool == ToolType.ATTACH) { // also for bind but im too lazy to implement bind rn
            blocks = null
            secondPos = buffer.readBlockPos()
            selectionDir = buffer.readEnum(Direction::class.java)
            secondDir = buffer.readEnum(Direction::class.java)
            shipId = buffer.readLong()
        } else if (tool == ToolType.WELD) {
            blocks = buffer.readNbt()
            secondPos = null
            selectionDir = buffer.readEnum(Direction::class.java)
            secondDir = null
            shipId = buffer.readLong()
        } else {
            blocks = null
            secondPos = null
            selectionDir = null
            secondDir = null
            shipId = null
        }
        onOff = buffer.readBoolean()
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            ClockworkModClient.WANDERWAND_EFFECT_RENDERER.handlePacket(this)
        }
        context.handled()
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(selectionPos)
        buffer.writeEnum(tool)
        if (tool == ToolType.SELECT || tool == ToolType.DESELECT) {
            buffer.writeNbt(blocks)
        } else if (tool == ToolType.ATTACH) {
            buffer.writeBlockPos(secondPos!!)
            buffer.writeEnum(selectionDir!!)
            buffer.writeEnum(secondDir!!)
            buffer.writeLong(shipId!!)
        } else if (tool == ToolType.WELD) {
            buffer.writeNbt(blocks)
            buffer.writeEnum(selectionDir!!)
            buffer.writeLong(shipId!!)
        }
        buffer.writeBoolean(onOff)
    }
}