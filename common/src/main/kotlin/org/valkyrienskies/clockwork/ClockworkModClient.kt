package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.event.events.common.TickEvent
import org.valkyrienskies.clockwork.ClockworkMod.Kelvin
import org.valkyrienskies.clockwork.content.forces.DragController
import org.valkyrienskies.clockwork.content.forces.PocketForcesController
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.ChuteSlotRenderer
import org.valkyrienskies.mod.common.shipObjectWorld

object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    fun initClient() {
        ClockworkPonderScenes.init()

        // This is really stupid, but it's how create does it, so ¯\_(ツ)_/¯
        TickEvent.PLAYER_POST.register() {
            ChuteSlotRenderer.tick()
        }
    }
}