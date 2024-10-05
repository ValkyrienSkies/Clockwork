package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import Effekseer.swig.EffekseerTextureType


@Suppress("unused")
enum class TextureType(val impl: EffekseerTextureType) {
    COLOR(EffekseerTextureType.Color),
    NORMAL(EffekseerTextureType.Normal),
    DISTORTION(EffekseerTextureType.Distortion);

    override fun toString(): String {
        return impl.toString()
    }

    val nativeOrdinal: Int
        get() = impl.swigValue()

    companion object {
        fun fromNativeOrdinal(ord: Int): TextureType? {
            for (value in entries) {
                if (value.nativeOrdinal == ord) {
                    return value
                }
            }
            return null
        }
    }
}