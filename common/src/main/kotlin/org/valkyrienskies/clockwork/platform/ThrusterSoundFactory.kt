package org.valkyrienskies.clockwork.platform

import dev.architectury.injectables.annotations.ExpectPlatform
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity
import org.valkyrienskies.clockwork.util.sound.GasThrusterSound

object ThrusterSoundFactory {
    @JvmStatic
    @ExpectPlatform
    fun create(be: GasThrusterBlockEntity): GasThrusterSound = throw AssertionError()
}
