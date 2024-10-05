package org.valkyrienskies.clockwork.effekseer.client.internal

import net.irisshaders.iris.api.v0.IrisApi

internal object IrisProxy {
    val isIrisShaderEnabled: Boolean
        get() = IrisApi.getInstance().isShaderPackInUse
}