package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import org.joml.Quaterniondc
import org.joml.Vector2dc
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.max
import kotlin.math.min

class GravitronState {
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

        @JvmStatic
        fun leftClickItem(player: Player, state: GravitronState): Boolean {
            val level = player.level()
            if (state.shipID != null && level is ServerLevel) {
                val shipId = state.shipID
                if (shipId != null) {
                    val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getById(shipId)
                    if (ship != null) {
                        if (SharedValues.gravitronHandler.isRegular) {
                            val lookDir = player.lookAngle.normalize().toJOML()
                            val magnitude = 6000 * ship.inertiaData.mass
                            val launchVec = lookDir.mul(magnitude)
                            ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId).applyInvariantForceToPos(ship.id, launchVec, state.shipGrabbedPos!!)
                            GrabTool.dropShip(player)
                        } else {
                            ship.isStatic = !ship.isStatic
                        }

                        level.playSound(
                            player,
                            player.blockPosition(),
                            ClockworkSounds.WAND_FINISH.mainEvent!!,
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                        return true
                    }
                }
            }
            return false
        }
    }
}
