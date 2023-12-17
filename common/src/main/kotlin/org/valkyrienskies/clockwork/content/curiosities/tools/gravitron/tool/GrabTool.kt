package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import com.simibubi.create.AllKeys
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducer.Companion.getOrCreate
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem.Companion.GravitronState
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem.Companion.getState
import org.valkyrienskies.clockwork.util.ClockworkUtils.readVec3
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML

class GrabTool : GravitronToolBase() {

    override fun handleRightClick(): Boolean {
        updateTargetPos()
        sendToServer(GravitronGrabPacket(clickedPos, clickedLocation, GRAB))
        return true
    }

    override fun handleMouseWheel(delta: Double): Boolean {
        return false
    }

    companion object {

        private fun playerRotToQuaternion(pitch: Double, yaw: Double): Quaterniond {
            return Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch))
        }

        private fun dropShip(s: GravitronState, level: ServerLevel) {
            val grabbedShipId = s.shipID
            if (grabbedShipId != null) {
                val loadedShip = level.shipObjectWorld.loadedShips.getById(grabbedShipId)
                if (loadedShip != null) {
                    val gravitronForceInducer = getOrCreate(loadedShip)
                    gravitronForceInducer.data = null
                }
            }

            s.grabbing = false
            s.shipID = null
            s.shouldDrop = false
        }

        private fun updateShip(s: GravitronState, level: ServerLevel, entity: Entity) {
            if (s.grabbing) {
                val shipId = s.shipID
                if (shipId != null) {
                    val shipUnloaded = level.shipObjectWorld.allShips.getById(shipId)
                    val ship = level.shipObjectWorld.loadedShips.getById(shipId)
                    if (ship != null && s.playerGrabbedRotation != null && s.shipGrabbedDistance != null && s.shipGrabbedPos != null && s.heldBlockPos != null) {
                        // Update Rot Values
                        val playerCurrentRotation = Vector2d(entity.xRot.toDouble(), entity.yRot.toDouble())
                        val origPlayerRot =
                            playerRotToQuaternion(
                                s.playerGrabbedRotation!!.x(),
                                s.playerGrabbedRotation!!.y()
                            ).normalize()
                        val newPlayerRot =
                            playerRotToQuaternion(playerCurrentRotation.x(), playerCurrentRotation.y()).normalize()
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
                    } else if (shipUnloaded == null) {
                        dropShip(s, level)
                    }
                }
            }
        }

        private fun updateShipDirection(s: GravitronState, level: ServerLevel, entity: Entity, dir: Direction) {
            if (s.grabbing) {
                val shipId = s.shipID
                if (shipId != null) {
                    val shipUnloaded = level.shipObjectWorld.allShips.getById(shipId)
                    val ship = level.shipObjectWorld.loadedShips.getById(shipId)
                    if (ship != null && s.playerGrabbedRotation != null && s.shipGrabbedDistance != null && s.shipGrabbedPos != null && s.heldBlockPos != null) {
                        // Update Rot Values
                        val lockedCurrentRotation = Vector2d(0.0, (dir.get2DDataValue() * 90).toDouble())
                        val origPlayerRot =
                            playerRotToQuaternion(
                                s.playerGrabbedRotation!!.x(),
                                s.playerGrabbedRotation!!.y()
                            ).normalize()
                        val newPlayerRot =
                            playerRotToQuaternion(lockedCurrentRotation.x(), lockedCurrentRotation.y()).normalize()
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
                    } else if (shipUnloaded == null) {
                        dropShip(s, level)
                    }
                }
            }
        }

        @JvmStatic
        fun tick(player: Player) {
            if (player.level() is ServerLevel) {
                val s = getState(player)
                val graviton = player.getMainHandItem()
                val serverLevel = player.level() as ServerLevel

                if (!s.shouldDrop && graviton.`is`(ClockworkItems.GRAVITRON.asItem())) {
                    if (AllKeys.ACTIVATE_TOOL.isPressed) {
                        updateShipDirection(s, serverLevel, player, player.getDirection())
                    } else {
                        updateShip(s, serverLevel, player)
                    }
                } else {
                    dropShip(s, serverLevel)
                }

                if (s.grabCD != null && s.grabCD!! > 0) {
                    s.grabCD = s.grabCD!! - 1
                }
                if (graviton.hasTag() && graviton.tag!!.contains("GrabbedPosInShip") && !player.cooldowns.isOnCooldown(
                        graviton.item
                    )
                ) {
                    s.grabbing = true
                    val tag = graviton.tag

                    val clickLocation = readVec3(tag!!.getList("GrabbedPosInShip", Tag.TAG_DOUBLE.toInt()))
                    val id = tag.getLong("ShipId")

                    val ship: LoadedServerShip? = serverLevel.shipObjectWorld.loadedShips.getById(id)
                    if (ship != null) {
                        val transformedPos = ship.worldToShip.transformPosition(clickLocation.toJOML(), Vector3d())
                        grabShip(s, player, ship, transformedPos)
                        graviton.removeTagKey("ShipId")
                        graviton.removeTagKey("GrabbedPosInShip")
                    }
                }
            }
        }

        @JvmStatic
        fun tryGrabShip(level: ServerLevel, player: Player, clickedPos: BlockPos, clickLocation: Vec3): Boolean {
            val chunkX = clickedPos.x shr 4
            val chunkZ = clickedPos.z shr 4
            val ship = level.shipObjectWorld.loadedShips.getByChunkPos(chunkX, chunkZ, level.dimensionId)
            val grabPosInShip: Vector3dc = clickLocation.toJOML()
            val grabPosInWorld = Vector3d(grabPosInShip)
            val s = getState(player)
            if (level.isBlockInShipyard(clickedPos) && ship == null) {
                return false
            }

            if (ship == null) {
                return false
            } else {
                ship.shipToWorld.transformPosition(grabPosInWorld)
            }
            grabShip(s, player, ship, grabPosInShip)
            return true
        }

        private fun grabShip(s: GravitronState, p: Player, ship: LoadedServerShip, grabPosInShip: Vector3dc) {
            val heldPosInWorld = Vector3d()
            ship.transform.shipToWorld.transformPosition(Vector3d(grabPosInShip), heldPosInWorld)

            s.shipID = ship.id
            s.heldBlockPos = heldPosInWorld
            s.playerGrabbedRotation = Vector2d(p.xRot.toDouble(), p.yRot.toDouble())
            s.shipGrabbedPos = Vector3d(grabPosInShip)
            s.shipGrabbedRot = ship.transform.shipToWorldRotation
            s.shipGrabbedDistance = p.eyePosition.toJOML().distance(heldPosInWorld)
            ship.isStatic = false
        }
    }


}