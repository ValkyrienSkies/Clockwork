package org.valkyrienskies.clockwork.content.curiosities.clock

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkSounds

class ClockBlockEntity(type: BlockEntityType<*>, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos,
    state
) {

    var secondHandTargetRotation = 0.0
    var minuteHandTargetRotation = 0.0
    var hourHandTargetRotation = 0.0

    var trailerAnimProgress = -1.0
    var doingTrailerAnim = false

    var tickTock = false
    var startedSong = false

    fun calcHandRotation(trailerAnim: Boolean = false) {
        if (!trailerAnim) {
            val time = level!!.dayTime

            val hours = (time / 2000)  // 1 hour = 2000 ticks
            val minutes = (time % 2000) / 33.333  // 1 minute = 33.333 ticks
            val seconds = (time % 33.333) * (60 / 33.333)  // Smooth seconds scaling

            secondHandTargetRotation = AngleHelper.rad((seconds % 60.0) * 6.0).toDouble()
            minuteHandTargetRotation = AngleHelper.rad((minutes % 60.0) * 6.0).toDouble()
            hourHandTargetRotation = AngleHelper.rad((hours % 12.0) * 30.0).toDouble()
//            val seconds = time / 20 % 20
//            val minutes = time / 2000  % 2000
//            val hours = time / 2000 % 12 * 30
//            secondHandTargetRotation = Math.toRadians(seconds * 360.0 / 20.0)
//            minuteHandTargetRotation = Math.toRadians(minutes * 360.0 / 60.0)
//            hourHandTargetRotation = Math.toRadians(hours * 360.0 / 12.0)
        } else {
            if (trailerAnimProgress > -1.0) {
                if (secondHandTargetRotation < Math.toRadians(390.0)) {
                    secondHandTargetRotation = Math.toRadians(trailerAnimProgress * 390.0)
                }
            } else {
                secondHandTargetRotation = 0.0
            }
        }
    }


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
    }

    fun triggerTrailerAnim() {
        doingTrailerAnim = true
        trailerAnimProgress = 0.0
    }

    override fun tick() {
        super.tick()
        if (level?.isClientSide == true) {
            if (!doingTrailerAnim && trailerAnimProgress > -1.0) {
                trailerAnimProgress = -1.0
            }
        } else {
            calcHandRotation(doingTrailerAnim)
            if ((Math.toDegrees(secondHandTargetRotation) % 12).toInt() == 0 && !doingTrailerAnim) {
                if (tickTock) {
                    tickTock = false
                    level!!.playSound(null, worldPosition, ClockworkSounds.TOCK.mainEvent!!, SoundSource.BLOCKS, 0.5f, 1f)
                } else {
                    tickTock = true
                    level!!.playSound(null, worldPosition, ClockworkSounds.TICK.mainEvent!!, SoundSource.BLOCKS, 0.5f, 1f)
                }
            }

            if (doingTrailerAnim && !startedSong) {
                startedSong = true
                level!!.playSound(null, worldPosition, ClockworkSounds.CLOCK_SONG.mainEvent!!, SoundSource.MUSIC, 1f, 1f)
            }
        }
    }
}
