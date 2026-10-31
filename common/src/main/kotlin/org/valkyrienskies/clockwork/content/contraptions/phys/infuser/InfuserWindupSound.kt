package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.mod.common.getLoadedShipManagingPos

internal class InfuserWindupSound(private val be: PhysicsInfuserBlockEntity) : AbstractTickableSoundInstance(
    ClockworkSounds.PHYSICS_INFUSER_WINDUP.mainEvent!!, SoundSource.BLOCKS, RandomSource.create()) {
    private val started = be.sequence?.startedAt

    init {
        volume = 0.7f
        pitch = if (be.sequence?.firstUse == true) 1f else 2f
        updatePosition()
    }

    fun cancel() = stop()

    override fun tick() {
        val sequence = be.sequence
        val now = be.level?.gameTime
        if (be.isRemoved || sequence == null || now == null || sequence.startedAt != started ||
            sequence.skippedAt >= 0 || sequence.age(now) >= sequence.impact) {
            stop()
            return
        }
        updatePosition()
    }

    private fun updatePosition() {
        val pos = be.blockPos
        val point = Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        (be.level as? ClientLevel)?.getLoadedShipManagingPos(pos)?.renderTransform?.shipToWorld?.transformPosition(point)
        x = point.x; y = point.y; z = point.z
    }
}
