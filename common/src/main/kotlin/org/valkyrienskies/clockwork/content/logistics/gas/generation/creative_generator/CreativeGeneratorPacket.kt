package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.collections.HashMap

class CreativeGeneratorPacket(var gasValues: HashMap<GasType, Int>, var temperature: Double, var blockPos: BlockPos): C2SCWPacket {


    constructor(buffer: FriendlyByteBuf) : this(

        (buffer.readNbt() ?: CompoundTag()).let {
            val map: HashMap<GasType, Int> = HashMap(GasTypeRegistry.getGasTypes().associateWith { 0 })
            for (key in it.allKeys) {
                val id = ResourceLocation.tryParse(key) ?: continue
                val gas = GasTypeRegistry.getGasType(id) ?: continue
                map[gas] = it.getInt(key)
            }
            map
       },

        buffer.readDouble(),
        buffer.readBlockPos()
    )


    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val level = context.sender.level()
            if (!level.hasChunkAt(blockPos)) return@enqueueWork
            val be = level.getBlockEntity(blockPos) as? CreativeGeneratorBlockEntity ?: return@enqueueWork
            if (!temperature.isFinite()) return@enqueueWork
            be.gasValues = HashMap(GasTypeRegistry.getGasTypes().associateWith { (gasValues[it] ?: 0).coerceIn(0, 8000) })
            be.temperature = temperature.coerceIn(0.0, 2000.0)
            be.notifyUpdate()
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        val compound = CompoundTag()
        for (gas in gasValues.keys) {
            compound.putInt(gas.resourceLocation.toString(), gasValues[gas] ?: 0)
        }
        buffer.writeNbt(compound)
        buffer.writeDouble(temperature)
        buffer.writeBlockPos(blockPos)
    }
}
