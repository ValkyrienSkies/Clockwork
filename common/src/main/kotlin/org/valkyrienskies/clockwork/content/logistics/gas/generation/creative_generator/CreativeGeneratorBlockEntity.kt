package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*
import kotlin.math.max

class CreativeGeneratorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {



    var gasValues: EnumMap<GasType, Int> = EnumMap(GasType.entries.associateWith { 0 }) // This makes an EnumMap of all 0s
    var tempearature: Double = 0.0
    var fuelTicks: Int = 0

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        val node = ClockworkMod.getKelvin().getNodeAt(blockPos.toJOMLD()) ?: return
        val volumes = node.network.getGasVolumesAt(getDuctNodePosition())

        for (gas in gasValues.keys) {
            if (gasValues[gas] == 0 ) continue
            val gasVolume: Double
            if (volumes[gas] == null) gasVolume = 0.0
            else gasVolume = volumes[gas]!!
            node.network.modGasVolumeOfTemperature(getDuctNodePosition(), gas, max(gasValues[gas]!!.toDouble()-gasVolume, 0.0),tempearature)
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