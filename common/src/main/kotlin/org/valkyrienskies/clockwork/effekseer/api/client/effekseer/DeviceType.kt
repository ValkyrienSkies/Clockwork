package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import Effekseer.swig.EffekseerCoreDeviceType


enum class DeviceType(private val impl: EffekseerCoreDeviceType) {
    UNKNOWN(EffekseerCoreDeviceType.Unknown),
    OPENGL(EffekseerCoreDeviceType.OpenGL);

    val nativeOrdinal: Int
        get() = impl.swigValue()

    companion object {
        fun fromNativeOrdinal(ord: Int): DeviceType {
            for (value in entries) {
                if (value.nativeOrdinal == ord) {
                    return value
                }
            }
            throw IllegalArgumentException("DeviceType.fromNativeOrdinal: ord = $ord")
        }
    }
}