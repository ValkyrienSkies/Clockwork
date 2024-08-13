package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs

class PumpDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState): KineticBlockEntity(typeIn, pos, state), IHeatableBlockEntity {



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

    override fun remove() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.remove()
    }

    override fun destroy() {
        ClockworkMod.getKelvin().removeNode(this.blockPos.toJOMLD())
        super.destroy()
    }

    override fun tick() {
        super.tick()

        if (this.level?.isClientSide == true) {
            return
        }


        if (ClockworkMod.getKelvin().nodes[blockPos.toJOMLD()]?.behavior == NodeBehaviorType.PUMP) {
            val pumpNode = (ClockworkMod.getKelvin().nodes[this.blockPos.toJOMLD()] as PumpDuctNode)
            pumpNode.pumpPressure  =  (abs(this.getSpeed()).toDouble() / 256.0) * maxPumpPressure

            if (pumpNode.getEdges().size >=2) pumpNode.pumpTarget = this.blockPos.relative(blockState.getValue(BlockStateProperties.FACING)).toJOMLD()
        }


    }

    companion object {
        const val maxPumpPressure: Double = 1023440.0
    }


    override fun getDuctNodePosition(): DuctNodePos {
        return this.blockPos.toJOMLD()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        return super<IHeatableBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}