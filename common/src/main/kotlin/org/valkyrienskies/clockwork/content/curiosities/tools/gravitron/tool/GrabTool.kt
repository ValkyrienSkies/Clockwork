package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronAction
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronAnimationPacket
import org.valkyrienskies.clockwork.content.forces.GravitronController.Companion.getOrCreate
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronLeftClickPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState.Companion.getState
import org.valkyrienskies.clockwork.util.ClockworkUtils.readVec3
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML

class GrabTool : GravitronToolBase() {

    override fun handleRightClick(isRegular: Boolean): Boolean {
        val lastClickedPos = clickedPos
        val lastClickedLocation = clickedLocation
        updateTargetPos(isRegular)
        if (clickedPos != null && clickedLocation != null) {
            sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GRAB))
        } else {
            sendToServer(GravitronGrabPacket(BlockPos.ZERO, Vec3.ZERO, GRAB))
        }

        return true
    }

    override fun handleLeftClick(
        isRegular: Boolean
    ): Boolean {
        updateTargetPos(isRegular)
        // The server can launch the held ship even when it has moved off the crosshair.
        sendToServer(GravitronLeftClickPacket(clickedPos ?: BlockPos.ZERO))

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
        fun dropShip(player: Player, notify: Boolean = true): Boolean {
            val level = player.level() as? ServerLevel ?: return false
            val state = getState(player)
            val id = state.shipID ?: return false
            level.shipObjectWorld.loadedShips.getById(id)?.let { getOrCreate(it).data = null }
            if (notify) GravitronAnimationPacket.send(player, GravitronAction.RELEASE, id, state.shipGrabbedPos)
            state.shipID = null
            state.shipGrabbedPos = null
            state.heldBlockPos = null
            state.shipGrabbedRot = null
            state.shipGrabbedDistance = null
            state.playerGrabbedRotation = null
            return true
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

        /** Observe equipment before handling packets too: a click can arrive before the next player tick. */
        fun updateEquipment(player: Player): Boolean {
            val level = player.level() as? ServerLevel ?: return false
            val state = getState(player)
            val item = player.mainHandItem.item
            val equipped = item.takeIf {
                (it == ClockworkItems.GRAVITRON.get() || it == ClockworkItems.CREATIVE_GRAVITRON.get()) &&
                    player.isAlive && !player.isSpectator
            }
            if (state.equippedItem != equipped || state.equippedSlot != player.inventory.selected) {
                dropShip(player)
                if (equipped != null) {
                    GravitronAnimationPacket.send(player, GravitronAction.DRAW)
                    level.playSound(null, player.blockPosition(), ClockworkSounds.GRAVITRON_START.mainEvent!!,
                        player.soundSource, 0.3f, 1f)
                } else if (state.equippedItem != null) {
                    level.playSound(null, player.blockPosition(), ClockworkSounds.GRAVITRON_SHUTDOWN.mainEvent!!,
                        player.soundSource, 0.5f, 1f)
                }
                state.equippedItem = equipped
                state.equippedSlot = player.inventory.selected
            }
            if (equipped == null) dropShip(player)
            return equipped != null
        }

        /**
         * Handles updating the ship with updateShipDirection and updateShip if there is a stored shipId.
         * Handles the queued grab from Grabssemble, is the Gravitron has nbt for it
         */
        @JvmStatic
        fun tick(player: Player) {
            if (player.level() is ServerLevel) {
                val s = getState(player)
                val graviton = player.mainHandItem
                val serverLevel = player.level() as ServerLevel

                if (!updateEquipment(player)) return
                val bl = graviton.`is`(ClockworkItems.GRAVITRON.get().asItem())
                val heldShip = s.shipID?.let { serverLevel.shipObjectWorld.loadedShips.getById(it) }
                if (s.shipID != null && (heldShip == null || heldShip.isStatic)) dropShip(player)
                if (heldShip != null && bl && heldShip.inertiaData.mass > ClockworkConfig.SERVER.maxGravitronMass * 1000.0) {
                    val anchor = s.shipGrabbedPos
                    dropShip(player, false)
                    overload(player, heldShip, anchor)
                }
                if (s.shipID != null) {
                    updateShip(s, serverLevel, player)
                }
                if (player.tickCount % 10 == 0) {
                    if (s.shipID != null && heldShip != null) {
                        GravitronAnimationPacket.send(player, GravitronAction.HOLD, heldShip.id, s.shipGrabbedPos,
                            loadOf(heldShip))
                    } else {
                        GravitronAnimationPacket.send(player, GravitronAction.IDLE)
                    }
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
                level.playSound(
                    null,
                    player.blockPosition(),
                    ClockworkSounds.GRAVITRON_RELEASE.mainEvent!!,
                    player.soundSource,
                    1f,
                    1f
                )
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

                if (mass > ClockworkConfig.SERVER.maxGravitronMass * 1000 * 0.9) {
                    player.displayClientMessage(
                        Component.literal("Ship's starting to get heavy! ${mass.toInt()} / ${ClockworkConfig.SERVER.maxGravitronMass * 1000}").withStyle(
                            Style.EMPTY.withColor(
                                ChatFormatting.GOLD
                            )
                        ), true
                    )
                }
                if (mass > ClockworkConfig.SERVER.maxGravitronMass * 1000) {
                    overload(player, ship, grabPosInShip)
                    player.displayClientMessage(
                        Component.literal("Ship too heavy! ${mass.toInt()} / ${ClockworkConfig.SERVER.maxGravitronMass * 1000}").withStyle(
                            Style.EMPTY.withColor(
                                ChatFormatting.RED
                            )
                        ), true
                    )
                    return false
                }
            }

            grabShip(player, ship, grabPosInShip)
            level.playSound(
                null,
                player.blockPosition(),
                ClockworkSounds.GRAVITRON_GRAB.mainEvent!!,
                player.soundSource,
                1f,
                1f
            )

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
            GravitronAnimationPacket.send(player, GravitronAction.GRAB, ship.id, grabPosInShip, loadOf(ship))
        }

        fun loadOf(ship: ServerShip): Float =
            (ship.inertiaData.mass / (ClockworkConfig.SERVER.maxGravitronMass * 1000.0).coerceAtLeast(1.0)).toFloat()

        fun overload(player: Player, ship: ServerShip, anchor: Vector3dc?) {
            GravitronAnimationPacket.send(player, GravitronAction.OVERLOAD, ship.id, anchor, loadOf(ship))
            player.level().playSound(null, player.blockPosition(), ClockworkSounds.GRAVITRON_SHUTDOWN.mainEvent!!,
                player.soundSource, 0.9f, 0.65f)
            player.cooldowns.addCooldown(player.mainHandItem.item, 32)
        }
    }


}
