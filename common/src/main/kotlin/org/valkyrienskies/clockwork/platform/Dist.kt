package org.valkyrienskies.clockwork.platform

import dev.architectury.injectables.annotations.ExpectPlatform

object Dist {
    @ExpectPlatform
    fun onClient(runnable: Runnable) {
        throw AssertionError()
    }

    @ExpectPlatform
    fun onDedicatedServer(runnable: Runnable) {
        throw AssertionError()
    }
}