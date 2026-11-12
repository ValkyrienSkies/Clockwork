package org.valkyrienskies.clockwork.content.curiosities.magnet

import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.mod.common.getLoadedShipManagingPos

class PolaritronSound private constructor(private val level: ClientLevel, private val pos: BlockPos) :
    AbstractTickableSoundInstance(ClockworkSounds.POLARITRON_HUM.mainEvent!!, SoundSource.BLOCKS, RandomSource.create()) {
    init {
        looping = true
        delay = 0
        volume = 0f
        updatePosition()
    }

    override fun canStartSilent() = true

    override fun tick() {
        val mc = Minecraft.getInstance()
        if (mc.level !== level || !level.hasChunkAt(pos)) { stop(); return }
        updatePosition()
        val state = level.getBlockState(pos)
        val block = state.block as? MagnetBlock
        val power = if (block?.electromagnetic == true && AllConfigs.client().enableAmbientSounds.get() && nearby())
            state.getValue(MagnetBlock.POWER) else 0
        val target = if (power > 0) 0.16f + 0.3f * power / 15f else 0f
        volume += (target - volume).coerceIn(-0.06f, 0.03f)
        pitch += (0.85f + 0.15f * power / 15f - pitch) * 0.15f
        if (power == 0 && volume <= 0.001f) stop()
    }

    private fun updatePosition() {
        val point = Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        level.getLoadedShipManagingPos(pos)?.renderTransform?.shipToWorld?.transformPosition(point)
        x = point.x; y = point.y; z = point.z
    }

    private fun nearby() = Minecraft.getInstance().gameRenderer.mainCamera.position.distanceToSqr(x, y, z) < 24.0 * 24.0

    companion object {
        private val sounds = HashMap<BlockPos, PolaritronSound>()

        fun start(pos: BlockPos) {
            val mc = Minecraft.getInstance()
            val level = mc.level ?: return
            if (!AllConfigs.client().enableAmbientSounds.get() || sounds.containsKey(pos) || sounds.size >= 16) return
            val sound = PolaritronSound(level, pos.immutable())
            if (!sound.nearby()) return
            sounds[pos.immutable()] = sound
            mc.soundManager.play(sound)
        }

        fun tickSounds() {
            val mc = Minecraft.getInstance()
            sounds.entries.removeIf { (_, sound) ->
                if (sound.level !== mc.level) { mc.soundManager.stop(sound); true }
                else sound.isStopped || !mc.soundManager.isActive(sound)
            }
        }
    }
}
