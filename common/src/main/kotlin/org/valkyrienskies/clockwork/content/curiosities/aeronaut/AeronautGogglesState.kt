package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.LivingEntity
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck

class AeronautGogglesState {

    var flapAngle = 0f
    var prevFlapAngle = 0f

    companion object {
        @JvmStatic
        fun getState(player: Player): AeronautGogglesState {
            val p = player as MixinPlayerDuck
            var s = p.getAeronautGogglesState()

            if (s == null) {
                s = AeronautGogglesState()
                p.setAeronautGogglesState(s)
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
        fun getGogglesAreDown(entity: LivingEntity): Boolean {
            return (entity as? MixinPlayerDuck)?.gogglesDown ?: false
        }
    }
}
