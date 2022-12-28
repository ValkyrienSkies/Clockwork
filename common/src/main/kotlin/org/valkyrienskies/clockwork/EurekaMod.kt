package org.valkyrienskies.clockwork

import org.valkyrienskies.core.impl.config.VSConfigClass


object EurekaMod {
    const val MOD_ID = "vs_clockwork"

    @JvmStatic
    fun init() {
        EurekaBlocks.register()
        EurekaBlockEntities.register()
        EurekaItems.register()
        EurekaScreens.register()
        EurekaEntities.register()
        EurekaWeights.register()
        VSConfigClass.registerConfig("vs_clockwork", EurekaConfig::class.java)
    }

    @JvmStatic
    fun initClient() {
        EurekaClientScreens.register()
    }
}
