package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctEdgeSyncPacket
import org.valkyrienskies.clockwork.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*

class CoalBurnerBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {


    var fuelTicks: Int = 0

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        val node = ClockworkMod.getKelvin().getNodeAt(blockPos.toJOMLD()) ?: return




        if (fuelTicks>0) {
            fuelTicks-=1
            if (node.network.getTemperatureAt(blockPos.toJOMLD())<450.0) node.network.modTemperature(blockPos.toJOMLD(),30.0)

            if (blockState.getValue(CoalBurnerBlock.LIT)==false) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,true), 15)
        } else {
            if (blockState.getValue(CoalBurnerBlock.LIT)==true) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,false), 15)
        }
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return this.blockPos.toJOMLD()
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

}