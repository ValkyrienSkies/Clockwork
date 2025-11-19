package org.valkyrienskies.clockwork

import com.simibubi.create.AllContraptionTypes
import com.simibubi.create.api.contraption.ContraptionType
import com.simibubi.create.content.contraptions.Contraption
import net.minecraft.core.Holder
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import java.util.function.Supplier

import com.simibubi.create.AllContraptionTypes.BY_LEGACY_NAME
import com.simibubi.create.api.registry.CreateBuiltInRegistries
import net.minecraft.core.Registry

object ClockworkContraptions {
    val FLAP = register(
        ClockworkMod.asResource("flap").toString()
    ) { FlapContraption() }
    val PROPELLER = register(
        ClockworkMod.asResource("propeller").toString()
    ) { PropellerContraption() }

    private fun register(name: String, factory: Supplier<out Contraption>): Holder.Reference<ContraptionType> {
        val type: ContraptionType = ContraptionType(factory)
        BY_LEGACY_NAME.put(name, type)

        return Registry.registerForHolder(CreateBuiltInRegistries.CONTRAPTION_TYPE, ClockworkMod.asResource(name), type)
    }

    @JvmStatic
    fun init() {}
}
