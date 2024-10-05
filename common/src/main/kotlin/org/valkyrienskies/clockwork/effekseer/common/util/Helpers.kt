package org.valkyrienskies.clockwork.effekseer.common.util

import org.valkyrienskies.clockwork.platform.NativePlatform
import java.util.function.Supplier

object Helpers {
    fun <T> checkPlatform(constructor: Supplier<T>): T {
        if (NativePlatform.isRunningOnUnsupportedPlatform) {
            throw UnsupportedOperationException("Unsupported platform")
        }
        return constructor.get()
    }
}