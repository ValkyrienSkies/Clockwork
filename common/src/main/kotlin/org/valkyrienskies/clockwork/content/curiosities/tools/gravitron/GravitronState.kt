package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import org.joml.Quaterniondc
import org.joml.Vector2dc
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.shipObjectWorld

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
        fun leftClickItem(player: Player, state: GravitronState): Boolean {
            val level = player.level()
            if (state.shipID != null && level is ServerLevel) {
                val shipId = state.shipID
                if (shipId != null) {
                    val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getById(shipId)
                    if (ship != null) {
                        ship.isStatic = !ship.isStatic
                        level.playSound(
                            player,
                            player.blockPosition(),
                            ClockworkSounds.DESIGNATOR_ACTIVATE.mainEvent!!,
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