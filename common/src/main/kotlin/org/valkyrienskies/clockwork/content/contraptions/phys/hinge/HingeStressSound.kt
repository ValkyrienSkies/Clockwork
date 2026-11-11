package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.mod.common.getLoadedShipManagingPos

internal class HingeStressSound(private val be: HingeBlockEntity) : AbstractTickableSoundInstance(
    ClockworkSounds.HINGE_STRESS.mainEvent!!, SoundSource.BLOCKS, RandomSource.create()) {
    init {
        looping = true
        delay = 0
        volume = 0.6f
        updatePosition()
    }

    override fun tick() {
        if (be.isRemoved || !be.strained || be.level?.getBlockEntity(be.blockPos) !== be) {
            stop()
            return
        }
        updatePosition()
    }

    private fun updatePosition() {
        val point = HingeLinks.center(be.blockPos)
        (be.level as? ClientLevel)?.getLoadedShipManagingPos(be.blockPos)?.renderTransform?.shipToWorld?.transformPosition(point)
        x = point.x; y = point.y; z = point.z
    }
}
