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
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*

class CoalBurnerBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {


    var fuelTicks: Int = 0

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(blockPos.toDuctNodePos(level!!.dimension().location())) ?: return




        if (fuelTicks>0) {
            fuelTicks-=1
            if (kelvin.getTemperatureAt(blockPos.toDuctNodePos(level!!.dimension().location()))<2000.0) kelvin.modTemperature(blockPos.toDuctNodePos(level!!.dimension().location()),30.0)

            if (blockState.getValue(CoalBurnerBlock.LIT)==false) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,true), 15)
        } else {
            if (blockState.getValue(CoalBurnerBlock.LIT)==true) level!!.setBlock(blockPos,blockState.setValue(CoalBurnerBlock.LIT,false), 15)
        }
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
    }

}