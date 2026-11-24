package org.valkyrienskies.clockwork.util.sound

import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.sounds.AudioStream
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity
import org.valkyrienskies.clockwork.platform.ThrusterSoundFactory
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt

abstract class GasThrusterSound(source: GasThrusterBlockEntity) :
    AbstractTickableSoundInstance(ClockworkSounds.THRUSTER.mainEvent!!, SoundSource.AMBIENT, RandomSource.create()) {
    private val thruster = WeakReference(source)
    private val seed = source.blockPos.asLong() xor 0x764DE1BL
    @Volatile private var controls = ThrusterControls.SILENT
    private var silentTicks = 0
    private var retryAt = 0L

    init {
        looping = true
        updatePosition(source)
    }

    protected fun openStream(): CompletableFuture<AudioStream> =
        CompletableFuture.completedFuture(ThrusterAudioStream(seed) { controls })

    private fun updatePosition(be: GasThrusterBlockEntity) {
        val point = Vector3d(be.blockPos.x + 0.5, be.blockPos.y + 0.5, be.blockPos.z + 0.5)
        (be.level as? ClientLevel)?.getLoadedShipManagingPos(be.blockPos)?.renderTransform?.shipToWorld?.transformPosition(point)
        x = point.x
        y = point.y
        z = point.z
    }

    private fun inRange() = Minecraft.getInstance().gameRenderer.mainCamera.position.distanceToSqr(x, y, z) < 32 * 32

    private fun updateControls(be: GasThrusterBlockEntity) {
        val flow = be.gasMassFlow.values.sum()
        controls = ThrusterControls(
            sqrt(flow / 0.004).coerceIn(0.0, 1.4), (be.velocity / 680.0).coerceIn(0.0, 1.4),
            be.plumeHeat.toDouble(), be.steamFraction.toDouble(), be.exhaustFraction.toDouble(), be.expansion.toDouble()
        )
    }

    override fun tick() {
        val be = thruster.get()
        if (be == null || be.isRemoved || be.level !== Minecraft.getInstance().level ||
            !AllConfigs.client().enableAmbientSounds.get()) {
            stop()
            return
        }
        updatePosition(be)
        if (!inRange()) {
            stop()
            return
        }
        updateControls(be)
        silentTicks = if (controls.flow == 0.0) silentTicks + 1 else 0
        if (silentTicks >= 7) stop()
    }

    companion object {
        private val sounds = WeakHashMap<GasThrusterBlockEntity, GasThrusterSound>()

        fun start(be: GasThrusterBlockEntity) {
            if (!AllConfigs.client().enableAmbientSounds.get()) return
            val mc = Minecraft.getInstance()
            val now = be.level?.gameTime ?: return
            val old = sounds[be]
            if (old != null && !old.isStopped && (now < old.retryAt || mc.soundManager.isActive(old))) return
            val sound = ThrusterSoundFactory.create(be)
            sound.retryAt = now + 20
            sounds[be] = sound
            if (sound.inRange()) {
                sound.updateControls(be)
                mc.soundManager.play(sound)
            }
        }
    }
}
