package org.valkyrienskies.clockwork.api.wheel

import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.content.physicalities.wheel.TireItem
import org.valkyrienskies.clockwork.content.physicalities.wheel.TireType

object TireTypes {
    private val types = TireType.builtIns.associateBy { it.id }.toMutableMap()
    private val items = mutableMapOf<ResourceLocation, TireItem>()

    @JvmStatic
    @Synchronized
    fun get(id: ResourceLocation): TireType? = types[id]

    @JvmStatic
    @Synchronized
    fun values(): List<TireType> = types.values.toList()

    @JvmStatic
    @Synchronized
    fun item(type: TireType): TireItem = items.getValue(type.id)

    @Synchronized
    internal fun register(item: TireItem) {
        val type = item.type
        require(type != TireType.WOOD && type.id !in items && (types[type.id] ?: type) === type) {
            "Tire type ${type.id} is already registered"
        }
        types[type.id] = type
        items[type.id] = item
    }
}
