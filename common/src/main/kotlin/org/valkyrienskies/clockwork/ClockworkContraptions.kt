package org.valkyrienskies.clockwork

import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.ContraptionType
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerContraption
import java.util.function.Supplier


object ClockworkContraptions {
    val FLAP = ContraptionType.register(ClockworkMod.asResource("flap").toString(),
        Supplier<Contraption> { FlapContraption() })
    val PROPELLOR = ContraptionType.register(ClockworkMod.asResource("propellor").toString(),
        Supplier<Contraption> { PropellerContraption() })

    fun init() {}
}