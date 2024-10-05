package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import java.util.*


/**
 * Wrapper for the int handle of native particle emitters.
 * @author ChloePrime
 */
@Suppress("unused")
class ParticleEmitter constructor(val handle: Int, private val manager: EffekseerManager, val type: Type) {
    enum class Type {
        WORLD,
        FIRST_PERSON_MAINHAND,
        FIRST_PERSON_OFFHAND,
    }

    var isVisible: Boolean = true
    var isPaused: Boolean = false

    private var callback: PreDrawCallback? = null

    init {
        setVisibility(true)
        resume()
    }

    fun pause() {
        manager.impl.SetPaused(this.handle, true)
        isPaused = true
    }

    fun resume() {
        manager.impl.SetPaused(this.handle, false)
        isPaused = false
    }

    fun setVisibility(visible: Boolean) {
        manager.impl.SetShown(this.handle, visible)
        isVisible = visible
    }

    fun stop() {
        manager.impl.Stop(this.handle)
    }

    fun setProgress(frame: Float) {
        manager.impl.UpdateHandleToMoveToFrame(this.handle, frame)
    }

    fun setPosition(x: Float, y: Float, z: Float) {
        manager.impl.SetEffectPosition(this.handle, x, y, z)
    }

    fun setRotation(x: Float, y: Float, z: Float) {
        manager.impl.SetEffectRotation(this.handle, x, y, z)
    }

    fun setScale(x: Float, y: Float, z: Float) {
        manager.impl.SetEffectScale(this.handle, x, y, z)
    }

    fun setTransformMatrix(matrix: FloatArray) {
        manager.impl.SetEffectTransformMatrix(
            this.handle,
            matrix[0], matrix[1], matrix[2], matrix[3],
            matrix[4], matrix[5], matrix[6], matrix[7],
            matrix[8], matrix[9], matrix[10], matrix[11]
        )
    }

    fun setTransformMatrix(matrix: Array<FloatArray>) {
        var i = 0
        manager.impl.SetEffectTransformMatrix(
            this.handle,
            matrix[i][0], matrix[i][1], matrix[i][2], matrix[i++][3],
            matrix[i][0], matrix[i][1], matrix[i][2], matrix[i++][3],
            matrix[i][0], matrix[i][1], matrix[i][2], matrix[i][3]
        )
    }

    fun setBaseTransformMatrix(matrix: FloatArray) {
        manager.impl.SetEffectTransformBaseMatrix(
            this.handle,
            matrix[0], matrix[1], matrix[2], matrix[3],
            matrix[4], matrix[5], matrix[6], matrix[7],
            matrix[8], matrix[9], matrix[10], matrix[11]
        )
    }

    fun setBaseTransformMatrix(matrix: Array<FloatArray>) {
        var i = 0
        manager.impl.SetEffectTransformBaseMatrix(
            this.handle,
            matrix[i][0], matrix[i][1], matrix[i][2], matrix[i++][3],
            matrix[i][0], matrix[i][1], matrix[i][2], matrix[i++][3],
            matrix[i][0], matrix[i][1], matrix[i][2], matrix[i][3]
        )
    }

    fun exists(): Boolean {
        return manager.impl.Exists(this.handle)
    }

    fun setDynamicInput(index: Int, value: Float) {
        manager.impl.SetDynamicInput(this.handle, index, value)
    }

    fun getDynamicInput(index: Int): Float {
        return manager.impl.GetDynamicInput(this.handle, index)
    }

    fun sendTrigger(index: Int) {
        manager.impl.SendTrigger(this.handle, index)
    }

    fun interface PreDrawCallback {
        fun accept(emitter: ParticleEmitter, partialTicks: Float)

        fun andThen(after: PreDrawCallback): PreDrawCallback {
            Objects.requireNonNull(after)
            return PreDrawCallback { emitter: ParticleEmitter, partial: Float ->
                accept(emitter, partial)
                after.accept(emitter, partial)
            }
        }
    }

    fun addPreDrawCallback(callback: PreDrawCallback) {
        Optional.ofNullable(this.callback).ifPresentOrElse(
            { c: PreDrawCallback ->
                this.callback =
                    this.callback!!.andThen(c)
            },
            { this.callback = callback }
        )
    }

    fun runPreDrawCallbacks(partial: Float) {
        Optional.ofNullable(this.callback).ifPresent { c: PreDrawCallback ->
            c.accept(
                this,
                partial
            )
        }
    }
}