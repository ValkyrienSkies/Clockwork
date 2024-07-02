package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeEdge
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createPipeNode
import org.valkyrienskies.mod.common.util.toJOMLD
import java.time.Clock

class DuctBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehavioursDeferred(behaviours)
    }

    override fun onLoad() {
        super.onLoad()
        ClockworkMod.getKelvin().markLoaded(this.blockPos.toJOMLD())
    }

    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        ClockworkMod.getKelvin().markUnloaded(this.blockPos.toJOMLD())
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("currentTemperature")) {
            ClockworkMod.getKelvin().nodeInfo[this.blockPos.toJOMLD()]?.currentTemperature = tag.getDouble("currentTemperature")
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putDouble("currentTemperature", ClockworkMod.getKelvin().getTemperatureAt(this.blockPos.toJOMLD()))
        super.write(tag, clientPacket)
    }

    override fun initialize() {
        super.initialize()

        if (this.level?.isClientSide == true) {
            return
        }

        ClockworkMod.getKelvin().addNode(this.blockPos.toJOMLD(), createPipeNode(this.blockPos.toJOMLD(), ClockworkMod.getKelvin()))
        for (dir in Direction.values()) {
            val block = this.level!!.getBlockState(this.blockPos).block
            val otherBlock = this.level!!.getBlockState(this.blockPos.relative(dir)).block
            if (block is IDuct && otherBlock is IDuct && block.canConnectTo(this.blockPos, this.blockPos.relative(dir), this.level!!) && otherBlock.canConnectTo(this.blockPos.relative(dir), this.blockPos, this.level!!)) {
                ClockworkMod.getKelvin().addEdge(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD(), createPipeEdge(this.blockPos.toJOMLD(), this.blockPos.relative(dir).toJOMLD()))
            }
        }
    }

    override fun remove() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.remove()
    }

    override fun destroy() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.destroy()
    }
}