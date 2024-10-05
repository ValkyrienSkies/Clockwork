package org.valkyrienskies.clockwork.effekseer.client.internal

import dev.architectury.platform.Platform


object RenderContext {
    @JvmStatic
    val IRIS_MODE: Boolean = Platform.isModLoaded("iris") || Platform.isModLoaded("oculus")
    @JvmStatic
    val ON_FABRIC: Boolean = Platform.isFabric()

    @JvmStatic
    fun renderLevelDeferred(): Boolean {
        return !IRIS_MODE || ON_FABRIC
    }

    @JvmStatic
    fun renderHandDeferred(): Boolean {
        return !IRIS_MODE || (ON_FABRIC || isIrisShaderEnabled)
    }

    @JvmStatic
    fun captureHandDepth(): Boolean {
        return !IRIS_MODE || !isIrisShaderEnabled
    }

    @JvmStatic
    val isIrisShaderEnabled: Boolean
        get() = IRIS_MODE && IrisProxy.isIrisShaderEnabled
}