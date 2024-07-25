package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.event.events.client.ClientTickEvent
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandEffectRenderer

object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDERWAND_EFFECT_RENDERER = WanderwandEffectRenderer()

    @JvmStatic
    fun initClient() {
        ClockworkPonderScenes.init()
        ClientTickEvent.CLIENT_LEVEL_POST.register {
            WANDERWAND_EFFECT_RENDERER.clientTick(it)
        }
    }
}