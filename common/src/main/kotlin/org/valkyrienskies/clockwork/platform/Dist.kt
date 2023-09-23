package org.valkyrienskies.clockwork.platform

import dev.architectury.injectables.annotations.ExpectPlatform

object Dist {
    @ExpectPlatform
    @JvmStatic
    fun onClient(runnable: Runnable) {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun onDedicatedServer(runnable: Runnable) {
        throw AssertionError()
    }
}