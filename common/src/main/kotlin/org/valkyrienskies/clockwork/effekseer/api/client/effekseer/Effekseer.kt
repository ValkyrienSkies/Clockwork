package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import Effekseer.swig.EffekseerBackendCore
import org.valkyrienskies.clockwork.effekseer.common.util.Helpers


/**
 * @author ChloePrime
 */
@Suppress("unused")
class Effekseer protected constructor(val impl: EffekseerBackendCore) :
    SafeFinalized<EffekseerBackendCore>(impl, { obj: EffekseerBackendCore -> obj.delete() }) {
    constructor() : this(Helpers.checkPlatform { EffekseerBackendCore() })

    override fun close() {
        impl.delete()
    }

    companion object {
        fun init(): Boolean {
            return EffekseerBackendCore.InitializeWithOpenGL()
        }

        fun terminate() {
            EffekseerBackendCore.Terminate()
        }

        val deviceType: DeviceType
            get() = DeviceType.fromNativeOrdinal(EffekseerBackendCore.GetDevice().swigValue())
    }
}