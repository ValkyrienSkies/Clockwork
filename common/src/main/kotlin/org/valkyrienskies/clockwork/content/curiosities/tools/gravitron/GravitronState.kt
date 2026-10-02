package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.world.entity.player.Player
import org.joml.Quaterniondc
import org.joml.Vector2dc
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.math.min

class GravitronState {
    var equippedItem: net.minecraft.world.item.Item? = null
    var equippedSlot: Int = -1
    var heldBlockPos: Vector3dc? = null
    var playerGrabbedRotation: Vector2dc? = null // Pitch , Yaw
    var shipGrabbedPos: Vector3dc? = null
    var shipGrabbedRot: Quaterniondc? = null
    var shipID: ShipId? = null
    var shipGrabbedDistance: Double? = null

    companion object {
        @JvmStatic
        fun getState(player: Player): GravitronState {
            val p = player as MixinPlayerDuck
            var s = p.getGravitronState()

            if (s == null) {
                s = GravitronState()
                p.setGravitronState(s)
            }

            return s
        }

        @JvmStatic
        fun getDialAngle(player: Player): Float {
            val p = player as MixinPlayerDuck
            val angle = p.gravitronDialAngle
            return angle
        }

        @JvmStatic
        fun getPrevDialAngle(player: Player): Float {
            val p = player as MixinPlayerDuck
            val angle = p.prevGravitronDialAngle
            return angle
        }

        @JvmStatic
        fun getNeedRefresh(player: Player): Boolean {
            val p = player as MixinPlayerDuck
            val bl = p.needsRefresh
            return bl
        }

        //value should be between 0 and 100
        @JvmStatic
        fun mapValueToAngle(value: Float): Float {
            return min( value * 3.5f + 10, 350f) // (350 - 10) / 100
        }
    }
}
