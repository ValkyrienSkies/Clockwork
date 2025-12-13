package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
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
        val volumes = kelvin.getGasMassAt(getDuctNodePosition())

        for (gas in gasValues.keys) {
            if (gasValues[gas] == 0 ) continue
            val gasVolume: Double
            if (volumes[gas] == null) gasVolume = 0.0
            else gasVolume = volumes[gas]!!
            kelvin.modGasMassOfTemperature(getDuctNodePosition(), gas, max(gasValues[gas]!!.toDouble()-gasVolume, 0.0),temperature)
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