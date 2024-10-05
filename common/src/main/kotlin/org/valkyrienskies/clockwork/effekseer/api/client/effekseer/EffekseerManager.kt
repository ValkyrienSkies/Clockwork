package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import Effekseer.swig.EffekseerManagerCore
import org.joml.Matrix4f
import org.valkyrienskies.clockwork.effekseer.common.util.Helpers
import java.nio.FloatBuffer

/**
 * @author ChloePrime
 */
@Suppress("unused")
class EffekseerManager protected constructor(val impl: EffekseerManagerCore) :
    SafeFinalized<EffekseerManagerCore>(impl, { obj: EffekseerManagerCore -> obj.delete() }) {
    constructor() : this(Helpers.checkPlatform { EffekseerManagerCore() })

    fun init(maxSprites: Int, srgb: Boolean): Boolean {
        return impl.Initialize(maxSprites, srgb)
    }

    fun init(maxSprites: Int): Boolean {
        return impl.Initialize(maxSprites)
    }

    fun update(delta: Float) {
        impl.Update(delta)
    }

    fun startUpdate() {
        impl.BeginUpdate()
    }

    fun endUpdate() {
        impl.EndUpdate()
    }

    override fun close() {
        impl.delete()
    }

    fun createParticle(effect: EffekseerEffect): ParticleEmitter {
        return createParticle(effect, ParticleEmitter.Type.WORLD)
    }

    fun createParticle(effect: EffekseerEffect, type: ParticleEmitter.Type): ParticleEmitter {
        val handle = impl.Play(effect.impl)
        return ParticleEmitter(handle, this, type)
    }

    fun stopAllEffects() {
        impl.StopAllEffects()
    }

    fun draw() {
        drawBack()
        drawFront()
    }

    fun drawBack() {
        impl.DrawBack()
    }

    fun drawFront() {
        impl.DrawFront()
    }

    fun setViewport(width: Int, height: Int) {
        impl.SetViewProjectionMatrixWithSimpleWindow(width, height)
    }

    fun setupWorkerThreads(count: Int) {
        impl.LaunchWorkerThreads(count)
    }

    fun setCameraMatrix(m: FloatArray) {
        impl.SetCameraMatrix(
            m[0], m[1], m[2], m[3],
            m[4], m[5], m[6], m[7],
            m[8], m[9], m[0xA], m[0xB],
            m[0xC], m[0xD], m[0xE], m[0xF]
        )
    }

    fun setCameraMatrix(m: Matrix4f) {
        val buffer = FloatBuffer.wrap(MATRIX_BUFFER.get())
        m[buffer]
        setCameraMatrix(buffer)
    }

    fun setCameraMatrix(buf: FloatBuffer) {
        impl.SetCameraMatrix(
            buf.get(), buf.get(), buf.get(), buf.get(),
            buf.get(), buf.get(), buf.get(), buf.get(),
            buf.get(), buf.get(), buf.get(), buf.get(),
            buf.get(), buf.get(), buf.get(), buf.get()
        )
    }

    fun setCameraParameter(frontX: Float, frontY: Float, frontZ: Float, posX: Float, posY: Float, posZ: Float) {
        impl.SetCameraParameter(frontX, frontY, frontZ, posX, posY, posZ)
    }

    fun setProjectionMatrix(m: Matrix4f) {
        val buffer = FloatBuffer.wrap(MATRIX_BUFFER.get())
        m[buffer]
        setProjectionMatrix(buffer)
    }

    fun setProjectionMatrix(buf: FloatBuffer) {
        impl.SetProjectionMatrix(
            buf.get(), buf.get(), buf.get(), buf.get(),
            buf.get(), buf.get(), buf.get(), buf.get(),
            buf.get(), buf.get(), buf.get(), buf.get(),
            buf.get(), buf.get(), buf.get(), buf.get()
        )
    }

    fun setProjectionMatrix(m: FloatArray) {
        impl.SetProjectionMatrix(
            m[0], m[1], m[2], m[3],
            m[4], m[5], m[6], m[7],
            m[8], m[9], m[0xA], m[0xB],
            m[0xC], m[0xD], m[0xE], m[0xF]
        )
    }

    companion object {
        private val MATRIX_BUFFER: ThreadLocal<FloatArray> = ThreadLocal.withInitial {
            FloatArray(
                16
            )
        }
    }
}