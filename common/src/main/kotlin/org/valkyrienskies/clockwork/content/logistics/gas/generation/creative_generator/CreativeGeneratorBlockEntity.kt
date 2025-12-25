package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import kotlin.collections.HashMap
import kotlin.math.max

class CreativeGeneratorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {



    var gasValues: HashMap<GasType, Int> = HashMap(GasTypeRegistry.GAS_TYPES.values.associateWith { 0 }) // This makes an EnumMap of all 0s
    var temperature: Double = 0.0


    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(blockPos.toDuctNodePos(level!!.dimension().location())) ?: return
        val masses = kelvin.getGasMassAt(getDuctNodePosition())

        for (gas in gasValues.keys) {
            if (gasValues[gas] == 0 ) continue
            val gasMass: Double
            if (masses[gas] == null) gasMass = 0.0
            else gasMass = masses[gas]!!
            kelvin.addGasAtTemperature(getDuctNodePosition(), gas, max(gasValues[gas]!!/1000.0-gasMass, 0.0),temperature)
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

        temperature = tag.getDouble("Temperature")
        val gasValuesTag = tag.get("GasValues") as? CompoundTag ?: return
         for (gasTypeLocation in gasValuesTag.allKeys) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(gasTypeLocation)) ?: continue
            gasValues[gasType] = gasValuesTag.getInt(gasTypeLocation)
        }

    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        val gasValuesTag = CompoundTag()
        gasValues.forEach { gasValuesTag.putInt(it.key.resourceLocation.toString(), it.value) }
        tag.put("GasValues", gasValuesTag)
        tag.putDouble("Temperature",temperature)

        super.write(tag, clientPacket)
    }

}
