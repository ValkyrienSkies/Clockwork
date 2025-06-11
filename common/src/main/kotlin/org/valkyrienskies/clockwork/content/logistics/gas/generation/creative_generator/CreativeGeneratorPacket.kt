package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import java.util.*
import kotlin.collections.HashMap

class CreativeGeneratorPacket(var gasValues: HashMap<GasType, Int>, var temperature: Double, var blockPos: BlockPos): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(

        buffer.readNbt()!!.let {
            val map: HashMap<GasType, Int> = HashMap(GasTypeRegistry.GAS_TYPES.values.associateWith { 0 })
            for (key in it.allKeys) {
                map[GasTypeRegistry.getGasType(ResourceLocation(key))!!] = it.getInt(key)
            }
            map
       },

        buffer.readDouble(),
        buffer.readBlockPos()
    )


    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val be = context.sender.level.getBlockEntity(blockPos) ?: return@enqueueWork
            val cBe = be as CreativeGeneratorBlockEntity
            cBe.gasValues = gasValues
            cBe.temperature = temperature
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        val compound = CompoundTag()
        //todo: figure out some way for other namespaces in this packet
        for (gas in gasValues.keys) {
            compound.putInt("kelvin:${gas.name.lowercase()}", gasValues[gas] ?: 0)
        }
        buffer.writeNbt(compound)
        buffer.writeDouble(temperature)
        buffer.writeBlockPos(blockPos)
    }
}