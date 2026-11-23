package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.simulatesGas
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import kotlin.collections.HashMap

class CreativeGeneratorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {



    var gasValues: HashMap<GasType, Int> = HashMap(GasTypeRegistry.GAS_TYPES.values.associateWith { 0 }) // This makes an EnumMap of all 0s
    var temperature: Double = 0.0


    override fun tick() {
        super.tick()
        val level = level ?: return
        if (!level.simulatesGas) return
        val kelvin = ClockworkMod.getKelvin(level)
        val pos = getDuctNodePosition()
        if (kelvin.getNodeAt(pos) == null) return
        val masses = kelvin.getGasMassAt(pos)
        for ((gas, target) in gasValues) {
            val missing = target / 1000.0 - (masses[gas] ?: 0.0)
            if (missing > 0.0) kelvin.addGasAtTemperature(pos, gas, missing, temperature)
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
