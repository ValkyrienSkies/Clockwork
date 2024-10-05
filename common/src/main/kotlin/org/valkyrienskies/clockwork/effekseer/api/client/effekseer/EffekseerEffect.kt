package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import Effekseer.swig.EffekseerEffectCore
import org.valkyrienskies.clockwork.effekseer.common.util.Helpers
import java.io.IOException
import java.io.InputStream

/**
 * @author ChloePrime
 */
@Suppress("unused")
class EffekseerEffect protected constructor(val impl: EffekseerEffectCore) :
    SafeFinalized<EffekseerEffectCore>(impl, { obj: EffekseerEffectCore -> obj.delete() }) {
    var isLoaded: Boolean = false
        private set

    constructor() : this(Helpers.checkPlatform { EffekseerEffectCore() })

    override fun close() {
        impl.delete()
    }

    @Throws(IOException::class)
    fun load(stream: InputStream, amplifier: Float): Boolean {
        val bytes = stream.readAllBytes()
        return load(bytes, bytes.size, amplifier)
    }

    fun load(data: ByteArray?, length: Int, amplifier: Float): Boolean {
        isLoaded = impl.Load(data, length, amplifier)
        return isLoaded
    }

    @Throws(IOException::class)
    fun loadTexture(stream: InputStream, index: Int, type: TextureType): Boolean {
        val bytes = stream.readAllBytes()
        return loadTexture(bytes, bytes.size, index, type)
    }

    fun loadTexture(data: ByteArray?, length: Int, index: Int, type: TextureType): Boolean {
        return impl.LoadTexture(data, length, index, type.impl)
    }

    @Throws(IOException::class)
    fun loadCurve(stream: InputStream, index: Int): Boolean {
        val bytes = stream.readAllBytes()
        return loadCurve(bytes, bytes.size, index)
    }

    fun loadCurve(data: ByteArray?, length: Int, index: Int): Boolean {
        return impl.LoadCurve(data, length, index)
    }

    @Throws(IOException::class)
    fun loadMaterial(stream: InputStream, index: Int): Boolean {
        val bytes = stream.readAllBytes()
        return loadMaterial(bytes, bytes.size, index)
    }

    fun loadMaterial(data: ByteArray?, length: Int, index: Int): Boolean {
        return impl.LoadMaterial(data, length, index)
    }

    @Throws(IOException::class)
    fun loadModel(stream: InputStream, index: Int): Boolean {
        val bytes = stream.readAllBytes()
        return loadModel(bytes, bytes.size, index)
    }

    fun loadModel(data: ByteArray?, length: Int, index: Int): Boolean {
        return impl.LoadModel(data, length, index)
    }

    fun isModelLoaded(index: Int): Boolean {
        return impl.HasModelLoaded(index)
    }

    fun isCurveLoaded(index: Int): Boolean {
        return impl.HasCurveLoaded(index)
    }

    fun isMaterialLoaded(index: Int): Boolean {
        return impl.HasMaterialLoaded(index)
    }

    fun isTextureLoaded(index: Int, type: TextureType): Boolean {
        return impl.HasTextureLoaded(index, type.impl)
    }

    fun curveCount(): Int {
        return impl.GetCurveCount()
    }

    fun modelCount(): Int {
        return impl.GetModelCount()
    }

    fun materialCount(): Int {
        return impl.GetMaterialCount()
    }

    fun textureCount(type: TextureType): Int {
        return impl.GetTextureCount(type.impl)
    }

    fun textureCount(): Int {
        var amt = 0
        for (value in TextureType.entries) {
            amt += impl.GetTextureCount(value.impl)
        }
        return amt
    }

    fun getTexturePath(index: Int, type: TextureType): String {
        return impl.GetTexturePath(index, type.impl)
    }

    fun getCurvePath(index: Int): String {
        return impl.GetCurvePath(index)
    }

    fun getMaterialPath(index: Int): String {
        return impl.GetMaterialPath(index)
    }

    fun getModelPath(index: Int): String {
        return impl.GetModelPath(index)
    }

    fun minTerm(): Int {
        return impl.GetTermMin()
    }

    fun maxTerm(): Int {
        return impl.GetTermMax()
    }
}