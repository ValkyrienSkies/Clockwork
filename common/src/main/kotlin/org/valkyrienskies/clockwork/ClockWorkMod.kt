package org.valkyrienskies.clockwork

import org.valkyrienskies.core.impl.config.VSConfigClass


object ClockWorkMod {
    const val MOD_ID = "vs_clockwork"

    @JvmStatic
    fun init() {
        ClockWorkBlocks.register()
        ClockWorkBlockEntities.register()
        ClockWorkItems.register()
        ClockWorkScreens.register()
        ClockWorkEntities.register()
        ClockWorkWeights.register()
        VSConfigClass.registerConfig("vs_clockwork", ClockWorkConfig::class.java)
    }

    @JvmStatic
    fun initClient() {
        ClockWorkClientScreens.register()
    }
}
