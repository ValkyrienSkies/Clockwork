package org.valkyrienskies.clockwork

import com.simibubi.create.content.contraptions.ContraptionType
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.contraptions.smart_propeller.SmartPropellerContraption

object ClockworkContraptions {
    val FLAP = ContraptionType.register(
        ClockworkMod.asResource("flap").toString()
    ) { FlapContraption() }
    val PROPELLOR = ContraptionType.register(
        ClockworkMod.asResource("propellor").toString()
    ) { PropellerContraption() }
    val SMART_PROPELLOR = ContraptionType.register(
        ClockworkMod.asResource("smart_propellor").toString()
    ) { SmartPropellerContraption() }

    fun init() {}
}
