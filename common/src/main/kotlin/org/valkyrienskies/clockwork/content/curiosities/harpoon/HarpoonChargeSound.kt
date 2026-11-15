package org.valkyrienskies.clockwork.content.curiosities.harpoon

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor

/** A single spring loop per charging launcher; stop as soon as charging stops. */
class HarpoonChargeSound(private val launcher: HarpoonBlockEntity) : AbstractTickableSoundInstance(
    ClockworkSounds.SPRING_OVERLOAD.mainEvent!!, SoundSource.BLOCKS, RandomSource.create()) {
    init {
        looping = true
        delay = 0
        volume = 0.35f
        tick()
    }

    override fun tick() {
        val level = launcher.level
        val point = if (level == null || level !== Minecraft.getInstance().level || launcher.isRemoved || !launcher.isCharging) null
            else WandAnchor.at(level, launcher.blockPos, launcher.blockState.getValue(BlockStateProperties.FACING)).world(level)
        if (point == null) {
            stop()
            launcher.chargingSoundStopped()
            return
        }
        x = point.x; y = point.y; z = point.z
        pitch = 0.75f + launcher.charge.toFloat() * 0.5f
    }

    companion object {
        fun play(launcher: HarpoonBlockEntity) {
            Minecraft.getInstance().soundManager.play(HarpoonChargeSound(launcher))
        }
    }
}
