package org.valkyrienskies.clockwork.util.gui

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

interface IHaveDuctStats {
    fun getInternalVolume(): Double {
        return 1.0
    }

    fun getMaximumPressure(): Double {
        return 16375049.0
    }

    fun getMaximumTemperature(): Double {
        return 1478.0
    }


    fun getProductionStats(): Map<ResourceLocation, ProductionInfo> {
        return emptyMap()
    }
    fun getAdditionalInfoLines(): List<Component> {
        return emptyList()
    }
}

data class ProductionInfo(
    val method : ProductionMethod,
    val type: ProductionType,
    val condition: Component = Component.empty()
)

enum class ProductionMethod(val langKey: String) {
    RPM("vs_clockwork.production_method.rpm"), PASSIVE("vs_clockwork.production_method.passive"), BOILER("vs_clockwork.production_method.boiler"),  OTHER("vs_clockwork.production_method.other")
}
enum class ProductionType(val langKey: String) {
    ALWAYS("vs_clockwork.production_context.always"), CONDITIONAL("vs_clockwork.production_context.conditional")
}

