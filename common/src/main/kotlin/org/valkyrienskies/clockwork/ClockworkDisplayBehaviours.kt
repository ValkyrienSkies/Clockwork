package org.valkyrienskies.clockwork

import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours.assignBlockEntity
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours.register
import com.simibubi.create.content.redstone.displayLink.target.SignDisplayTarget
import org.valkyrienskies.clockwork.content.contraptions.phys.speed_gauge.SpeedGaugeDisplayTarget

object ClockworkDisplayBehaviours {


    fun init() {

        assignBlockEntity(register(ClockworkMod.asResource("speed_gauge_display_target"), SpeedGaugeDisplayTarget()), ClockworkBlockEntities.SPEED_GAUGE.get())
    }
}