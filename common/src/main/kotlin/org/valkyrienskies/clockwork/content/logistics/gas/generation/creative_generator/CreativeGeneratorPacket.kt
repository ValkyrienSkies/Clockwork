package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import java.util.*

class CreativeGeneratorPacket(var gasValues: EnumMap<GasType, Int>, var temperature: Double, var blockPos: BlockPos): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(

        buffer.readNbt()!!.let {
            val map: EnumMap<GasType, Int> = EnumMap(GasType.entries.associateWith { 0 })
            for (key in it.allKeys) {
                map[GasType.valueOf(key)] = it.getInt(key)
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
        for (gas in gasValues.keys) {
            compound.putInt(gas.name, gasValues[gas] ?: 0)
        }
        buffer.writeNbt(compound)
        buffer.writeDouble(temperature)
        buffer.writeBlockPos(blockPos)
    }
}