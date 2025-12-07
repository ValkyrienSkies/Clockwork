package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck

class AeronautGogglesState {

    var gogglesDown = false
    var flapAngle = 0f
    var prevFlapAngle = 0f

    companion object {
        @JvmStatic
        fun getState(player: Player): AeronautGogglesState {
            val p = player as MixinPlayerDuck
            var s = p.aeronautGogglesState

            if (s == null) {
                s = AeronautGogglesState()
                p.aeronautGogglesState = s
            }

            return s
        }

        @JvmStatic
        fun getFlapsAngle(player: Player): Float {
            val p = player as MixinPlayerDuck
            val angle = p.flapsAngle
            return angle
        }

        @JvmStatic
        fun getPrevFlapsAngle(player: Player): Float {
            val p = player as MixinPlayerDuck
            val angle = p.prevFlapsAngle
            return angle
        }

        @JvmStatic
        fun getGogglesAreDown(player: Player): Boolean {
            val p = player as MixinPlayerDuck
            val down = p.gogglesDown
            return down
        }
    }
}
