package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronDialPacket
import org.valkyrienskies.clockwork.content.forces.GravitronController.Companion.getOrCreate
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState.Companion.getState
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState.Companion.mapValueToAngle
import org.valkyrienskies.clockwork.util.ClockworkUtils.readVec3
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML

class GrabTool : GravitronToolBase() {

    override fun handleRightClick(): Boolean {
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GRAB))
        }

        return true
    }

    companion object {

        /**
         * Converts a pitch and yaw rotation to Quaterniond
         */
        private fun playerRotToQuaternion(pitch: Double, yaw: Double): Quaterniond {
            return Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch))
        }

        /**
         * Will nullify the force inducer and effectively returning the ship to its regular physics, a success will return true
         */
        fun dropShip(player: Player) : Boolean{
            if (getState(player).shipID != null && player.level is ServerLevel) {
                val serverLevel = player.level as ServerLevel
                val ship = serverLevel.shipObjectWorld.loadedShips.getById(getState(player).shipID!!)
                if (ship != null) {
                    val gravitronForceInducer = getOrCreate(ship)
                    gravitronForceInducer.data = null
                    getState(player).shipID = null
                    return true
                }
            }
            return false
        }

        /**
         * Controls the ships position and rotation when a player is grabbing the ship with the Gravitron.
         * The force calculated will be applied with GravitronForceInducer
         */
        private fun updateShipCommon(s: GravitronState, level: ServerLevel, entity: Entity, customRotation: Vector2d?) {
            if (s.shipID != null) {
                val ship = level.shipObjectWorld.loadedShips.getById(s.shipID!!)
                if (ship != null && s.playerGrabbedRotation != null && s.shipGrabbedDistance != null
                    && s.shipGrabbedPos != null && s.heldBlockPos != null
                ) {
                    val playerCurrentRotation = customRotation ?: Vector2d(entity.xRot.toDouble(), entity.yRot.toDouble())
                    val origPlayerRot = playerRotToQuaternion(s.playerGrabbedRotation!!.x(), s.playerGrabbedRotation!!.y()).normalize()
                    val newPlayerRot = playerRotToQuaternion(playerCurrentRotation.x(), playerCurrentRotation.y()).normalize()
                    val deltaPlayerRot = newPlayerRot.mul(origPlayerRot.conjugate(Quaterniond()), Quaterniond())
                    val rotation = deltaPlayerRot.mul(s.shipGrabbedRot, Quaterniond()).normalize()

                    // Update Pos Values
                    val lookDif = entity.lookAngle.toJOML().normalize().mul(s.shipGrabbedDistance!!)
                    s.heldBlockPos = entity.eyePosition.toJOML().add(lookDif)
                    val location = Vector3d(s.shipGrabbedPos)
                    val position = Vector3d(s.heldBlockPos)

                    val gravitronForceInducer = getOrCreate(ship)
                    val newData = GravitronForceInducerData(position, rotation, location)
                    gravitronForceInducer.data = newData
                }
            }
        }

        /**
         * Used with updateShipCommon to use the players rotation to determine the ships relative rotation
         */
        private fun updateShip(s: GravitronState, level: ServerLevel, entity: Entity) {
            updateShipCommon(s, level, entity, null)
        }

        /**
         * Used with updateShipCommon with an added Direction to lock the ships direction
         */
        private fun updateShipDirection(s: GravitronState, level: ServerLevel, entity: Entity, dir: Direction) {
            val lockedCurrentRotation = Vector2d(0.0, (dir.get2DDataValue() * 90).toDouble())
            updateShipCommon(s, level, entity, lockedCurrentRotation)
        }

        /**
         * Handles updating the ship with updateShipDirection and updateShip if there is a stored shipId.
         * Handles the queued grab from Grabssemble, is the Gravitron has nbt for it
         */
        @JvmStatic
        fun tick(player: Player) {
            if (player.level is ServerLevel) {
                val s = getState(player)
                val graviton = player.mainHandItem
                val serverLevel = player.level as ServerLevel

                var bl = graviton.`is`(ClockworkItems.GRAVITRON.get().asItem())
                var bl2 = graviton.`is`(ClockworkItems.CREATIVE_GRAVITRON.get().asItem())
                if (s.shipID != null && (bl || bl2)) {
                    updateShip(s, serverLevel, player)
                }

                if (graviton.hasTag() && graviton.tag!!.contains("GrabbedPosInShip") && !player.cooldowns.isOnCooldown(graviton.item)) {
                    val tag = graviton.tag

                    val clickLocation = readVec3(tag!!.getList("GrabbedPosInShip", Tag.TAG_DOUBLE.toInt()))
                    val id = tag.getLong("ShipId")

                    val ship: LoadedServerShip? = serverLevel.shipObjectWorld.loadedShips.getById(id)
                    if (ship != null) {
                        val transformedPos = ship.worldToShip.transformPosition(clickLocation.toJOML(), Vector3d())
                        grabShip(player, ship, transformedPos)
                        graviton.removeTagKey("ShipId")
                        graviton.removeTagKey("GrabbedPosInShip")
                    }
                }
            }
        }

        /**
         * Drops a ship if one is stored.
         * Tries to grab a ship with a given Position
         */
        @JvmStatic
        fun tryGrabShip(level: ServerLevel, player: Player, clickedPos: BlockPos, clickLocation: Vec3, isCreative: Boolean): Boolean {

            if (dropShip(player)) {
                return true
            }

            val ship = level.getShipManagingPos(clickedPos)
            val grabPosInShip: Vector3dc = clickLocation.toJOML()
            val grabPosInWorld = Vector3d(grabPosInShip)

            if (level.isBlockInShipyard(clickedPos) && ship == null) {
                return false
            }

            if (ship == null) {
                return false
            } else {
                ship.shipToWorld.transformPosition(grabPosInWorld)
            }

            if (!isCreative) {

                val mass = ship.inertiaData.mass
                if (player is ServerPlayer) {
                    val q = mass.toFloat() / (ClockworkConfig.SERVER.maxGravitronMass * 1000f)
                    val angle = mapValueToAngle(q * 100)
                    ClockworkPackets.sendTo(GravitronDialPacket(angle), player)
                }

                if (mass > ClockworkConfig.SERVER.maxGravitronMass * 1000 * 0.9) {
                    player.displayClientMessage(
                        TextComponent("Ship's starting to get heavy! ${mass.toInt()} / ${ClockworkConfig.SERVER.maxGravitronMass * 1000}").withStyle(
                            Style.EMPTY.withColor(
                                ChatFormatting.GOLD
                            )
                        ), true
                    )
                }
                if (mass > ClockworkConfig.SERVER.maxGravitronMass * 1000) {
                    player.displayClientMessage(
                        TextComponent("Ship too heavy! ${mass.toInt()} / ${ClockworkConfig.SERVER.maxGravitronMass * 1000}").withStyle(
                            Style.EMPTY.withColor(
                                ChatFormatting.RED
                            )
                        ), true
                    )
                    return false
                }
            }

            grabShip(player, ship, grabPosInShip)

            return true
        }

        /**
         * Grab a ship, makes ship not static and stores some data into GravitronState
         */
        private fun grabShip(player: Player, ship: ServerShip, grabPosInShip: Vector3dc) {
            val s = getState(player)
            val heldPosInWorld = Vector3d()
            ship.transform.shipToWorld.transformPosition(Vector3d(grabPosInShip), heldPosInWorld)

            s.shipID = ship.id
            s.heldBlockPos = heldPosInWorld
            s.playerGrabbedRotation = Vector2d(player.xRot.toDouble(), player.yRot.toDouble())
            s.shipGrabbedPos = Vector3d(grabPosInShip)
            s.shipGrabbedRot = ship.transform.shipToWorldRotation
            s.shipGrabbedDistance = player.eyePosition.toJOML().distance(heldPosInWorld)
            ship.isStatic = false
        }
    }


}