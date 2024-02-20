package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.core.api.ships.datastructures.AirPocket
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.datastructures.AirPocketImpl
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil

class AirPocketSyncPacket : S2CCWPacket {
    private val ship: Long
    private val id: Int
    private val pocket: ByteArray
    private val extraDataTag: CompoundTag

    constructor(shipId: ShipId, airPocket: AirPocket) {
        this.ship = shipId
        this.id = airPocket.id
        this.pocket = mapper.writeValueAsBytes(SyncableAirPocketData(shipId, airPocket.id, airPocket.pocket))
        this.extraDataTag = extraDataToCompoundTag(airPocket.extraData)
    }
    constructor(buffer: FriendlyByteBuf) {
        this.ship = buffer.readLong()
        this.id = buffer.readInt()
        this.pocket = buffer.readByteArray()
        this.extraDataTag = buffer.readNbt()!!
    }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            val airPocket = deserializeAirPocket(pocket, extraDataTag.getCompound(pocket.toString()))
            ClientAirPocketStorage.setAirPocket(ship, airPocket)
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeLong(ship)
        buffer.writeInt(id)
        buffer.writeByteArray(pocket)
        buffer.writeNbt(extraDataTag)
    }



    companion object {
        private fun serializeAirPocket(shipId: ShipId, airPocket: AirPocket): ByteArray {
            return mapper.writeValueAsBytes(SyncableAirPocketData(shipId, airPocket.id, airPocket.pocket))
        }

        private fun deserializeAirPocket(arr: ByteArray, extraData: CompoundTag): AirPocket {
            val data = mapper.readValue(arr, SyncableAirPocketData::class.java)
            return AirPocketImpl(data.pocketId, data.airPockets, compoundTagToExtraData(extraData))
        }

        private fun extraDataToCompoundTag(extraData: HashMap<String, Any>): CompoundTag {
            if (extraData.isEmpty()) {
                return CompoundTag()
            }
            val tag = CompoundTag()
            if (extraData.containsKey("kelvin/temperature_dbl_mrg_avg")) {
                tag.putDouble("kelvin/temperature_dbl_mrg_avg", extraData["kelvin/temperature_dbl_mrg_avg"] as Double)
            }
            if (extraData.containsKey("kelvin/gas_masses")) {
                val gasMasses = extraData["kelvin/gas_masses"] as HashMap<GasType, Double>
                val gasMassesTag = CompoundTag()
                for (gas in gasMasses.keys) {
                    gasMassesTag.putDouble(gas.name, gasMasses[gas]!!)
                }
                tag.put("kelvin/gas_masses", gasMassesTag)
            }
            return tag
        }

        private fun compoundTagToExtraData(compoundTag: CompoundTag): HashMap<String, Any> {
            val extraData = HashMap<String, Any>()

            if (compoundTag.contains("kelvin/temperature_dbl_mrg_avg")) {
                extraData["kelvin/temperature_dbl_mrg_avg"] = compoundTag.getDouble("kelvin/temperature_dbl_mrg_avg")
            }
            if (compoundTag.contains("kelvin/gas_masses")) {
                val gasMassesTag = compoundTag.getCompound("kelvin/gas_masses")
                val gasMasses = HashMap<GasType, Double>()
                for (gas in GasType.values()) {
                    if (gasMassesTag.contains(gas.name)) {
                        gasMasses[gas] = gasMassesTag.getDouble(gas.name)
                    }
                }
                extraData["kelvin/gas_masses"] = gasMasses
            }
            return extraData
        }
        val mapper = VSJacksonUtil.defaultMapper
    }
}