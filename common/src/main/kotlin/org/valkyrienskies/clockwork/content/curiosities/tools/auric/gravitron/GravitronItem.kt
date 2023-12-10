package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.content.schematics.SchematicItem
import com.simibubi.create.content.schematics.client.SchematicHandler
import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.*
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.AreaData
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import java.lang.Math.toRadians
import java.util.function.Consumer

class GravitronItem(properties: Properties) : CWItem(properties), CustomArmPoseItem {

    private var cooldown = 20

    private fun getState(player: Player): GravitronState {
        val p: MixinPlayerDuck = player as MixinPlayerDuck
        var s = p.cw_getGravitronState()
        if (s == null) {
            s = GravitronState()
            p.cw_setGravitronState(s)
        }

        return s
    }

    // Freeze the ship when player clicks
    fun leftClickItem(player: Player): Boolean {
        val s: GravitronState = getState(player)
        val level = player.level()
        if (s.grabbing && level is ServerLevel) {
            val shipId = s.shipID
            if (shipId != null) {
                val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getById(shipId)
                if (ship != null) {
                    ship.isStatic = !ship.isStatic
                    if (ship.isStatic) {
                        dropShip(s, level)
                    }
                    level.playSound(player, player.blockPosition(), ClockworkSounds.DESIGNATOR_ACTIVATE.mainEvent!!, SoundSource.PLAYERS, 1f, 1f)
                    return true
                }
            }
        }
        return false
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player
        val level = context.level
        if (player != null) {
            val s: GravitronState = getState(player)
            if ((s.shipID == null) && !player.cooldowns.isOnCooldown(this) && !s.grabbing) {
                if (level is ServerLevel) {
                    s.grabbing = true
                    player.cooldowns.addCooldown(this, cooldown)
                    s.grabCD = cooldown
                }
                tryAssembleAndGrabShip(level, player, context.clickedPos, context.clickLocation)
            }
        }
        return super.useOn(context)
    }

    //Sterner test code
    fun assemble(level: Level, player: Player, blockPos: BlockPos, clickLocation: Vec3): Boolean {
        val data = AreaData.of(player).get()
        val list = data.area

        list.selectionClusters.forEach { cluster ->
            val selection: DenseBlockPosSet = SelectedAreaToolkit.denseBlocksFromCluster(cluster)

            if (selection.isEmpty() || !selection.contains(blockPos.x, blockPos.y, blockPos.z)) {
                return@forEach
            }

            //data.setArea(SelectedAreaToolkit())

            selection.forEach { x, y, z ->
                if (level is ServerLevel) {
                    val serverLevel = level
                    if (!serverLevel.getBlockState(BlockPos(x, y, z)).isAir) {
                        val connectedShip = createNewShipWithBlocks(blockPos, selection, serverLevel)

                        val caughtEntities = SelectedAreaToolkit.entitiesFromCluster(cluster, serverLevel)
                        if (caughtEntities.isNotEmpty()) {
                            caughtEntities.forEach(Consumer { entity: Entity ->
                                if (entity is AbstractContraptionEntity || entity is SuperGlueEntity || entity is SeatEntity) {
                                    if (entity !is SuperGlueEntity) {
                                        val oldPos: Vector3dc = entity.position().toJOML()
                                        val newPos: Vector3dc =
                                            connectedShip.transform.worldToShip.transformPosition(oldPos, Vector3d())
                                        entity.moveTo(newPos.toMinecraft())
                                    } else {
                                        val oldBounds = entity.boundingBox
                                        val oldMax: Vector3dc = Vector3d(oldBounds.maxX, oldBounds.maxY, oldBounds.maxZ)
                                        val oldMin: Vector3dc = Vector3d(oldBounds.minX, oldBounds.minY, oldBounds.minZ)
                                        val newMax: Vector3dc =
                                            connectedShip.transform.worldToShip.transformPosition(oldMax, Vector3d())
                                        val newMin: Vector3dc =
                                            connectedShip.transform.worldToShip.transformPosition(oldMin, Vector3d())
                                        val newBounds =
                                            AABB(newMin.x(), newMin.y(), newMin.z(), newMax.x(), newMax.y(), newMax.z())
                                        entity.boundingBox = newBounds
                                        entity.resetPositionToBB()
                                    }
                                }
                            })
                        }

                        val grabPosInShip: Vec3 = clickLocation
                        val tag = player.mainHandItem.orCreateTag
                        tag.putLong("ShipId", connectedShip.id)
                        tag.put("GrabbedPosInShip", ClockworkUtils.writeVec3(grabPosInShip))

                        return true
                    }
                }
                //AreaData.of(player).get().shouldReset(list.selectionClusters)
            }
            data.area.toStopRendering.add(cluster)
            data.shouldReset(true)
        }
        //data.shouldReset(list.selectionClusters)

        return false
    }

    // || ITEM FUNCTIONS || //
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val s: GravitronState = getState(player)
        if ((s.shipID != null) && (s.grabCD == 0) && s.grabbing) {
            s.shouldDrop = true
        }
        return super.use(level, player, usedHand)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        if (entity !is Player || level !is ServerLevel) {
            return
        }
        val s: GravitronState = getState(entity)
        if (isSelected && !s.shouldDrop) {
            updateShip(s, level, entity)
        } else {
            dropShip(s, level)
        }
        if (s.grabCD!! > 0) {
            s.grabCD = s.grabCD!! - 1
        }
        if (stack.hasTag() && stack.tag!!.contains("GrabbedPosInShip") && !entity.cooldowns.isOnCooldown(stack.item)) {
            s.grabbing = true
            val tag = stack.tag!!

            val clickLocation = ClockworkUtils.readVec3(tag.getList("GrabbedPosInShip", Tag.TAG_DOUBLE.toInt()))
            val id = tag.getLong("ShipId")

            val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getById(id)
            if (ship != null) {
                val transformedPos = ship.worldToShip.transformPosition(clickLocation.toJOML(), Vector3d())
                grabShip(s, entity, ship, transformedPos)
                stack.removeTagKey("ShipId")
                stack.removeTagKey("GrabbedPosInShip")
            }
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    // called first to put the ship into the players grasp
    private fun tryGrabShip(level: ServerLevel, player: Player, clickedPos: BlockPos, clickLocation: Vec3): Boolean {
        val chunkX = clickedPos.x shr 4
        val chunkZ = clickedPos.z shr 4
        val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getByChunkPos(chunkX, chunkZ, level.dimensionId)
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

    private fun tryAssembleAndGrabShip(level: Level, player: Player, clickedPos: BlockPos, clickLocation: Vec3) {
        val bl = assemble(level, player, clickedPos, clickLocation)
        if (level is ServerLevel) {
            if (!bl) {
                val bl2 = tryGrabShip(level, player, clickedPos, clickLocation)
                if (!bl2) {
                    getState(player).grabbing = false
                    getState(player).shipID = null
                }
            } else {
                val p: MixinPlayerDuck = player as MixinPlayerDuck
                p.cw_setGravitronState(GravitronState())
            }
        }
    }

    // || SHIP FUNCTIONS || //
    private fun grabShip(
        s: GravitronState,
        p: Player,
        ship: LoadedServerShip,
        grabPosInShip: Vector3dc,
    ) {
        val heldPosInWorld: Vector3dc = ship.transform.shipToWorld.transformPosition(Vector3d(grabPosInShip))
        s.shipID = ship.id
        s.heldBlockPos = heldPosInWorld
        s.playerGrabbedRotation = Vector2d(p.xRot.toDouble(), p.yRot.toDouble())
        s.shipGrabbedPos = Vector3d(grabPosInShip)
        s.shipGrabbedRot = ship.transform.shipToWorldRotation
        s.shipGrabbedDistance = p.eyePosition.toJOML().distance(heldPosInWorld)
        ship.isStatic = false
    }

    // sets down the ship
    private fun dropShip(s: GravitronState, level: ServerLevel) {
        val grabbedShipId = s.shipID
        if (grabbedShipId != null) {
            val loadedShip = level.shipObjectWorld.loadedShips.getById(grabbedShipId)
            if (loadedShip != null) {
                val gravitronForceInducer = GravitronForceInducer.getOrCreate(loadedShip)
                gravitronForceInducer.data = null
            }
        }

        s.grabbing = false
        s.shipID = null
        s.shouldDrop = false
    }

    private fun updateShip(
        s: GravitronState,
        level: ServerLevel,
        entity: Entity,
    ) {
        if (s.grabbing) {
            val shipId = s.shipID
            if (shipId != null) {
                val shipUnloaded: Ship? = level.shipObjectWorld.allShips.getById(shipId)
                val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getById(shipId)
                if (ship != null) {
                    // Update Rot Values
                    val playerCurrentRotation: Vector2dc = Vector2d(entity.xRot.toDouble(), entity.yRot.toDouble())
                    val origPlayerRot: Quaterniondc =
                        playerRotToQuaternion(s.playerGrabbedRotation!!.x(), s.playerGrabbedRotation!!.y()).normalize()
                    val newPlayerRot: Quaterniondc =
                        playerRotToQuaternion(playerCurrentRotation.x(), playerCurrentRotation.y()).normalize()
                    val deltaPlayerRot: Quaterniondc =
                        newPlayerRot.mul(origPlayerRot.conjugate(Quaterniond()), Quaterniond())
                    val rotation: Quaterniondc = deltaPlayerRot.mul(s.shipGrabbedRot, Quaterniond()).normalize()

                    // Update Pos Values
                    val lookDif = entity.lookAngle.toJOML().normalize().mul(s.shipGrabbedDistance!!)
                    s.heldBlockPos = entity.eyePosition.toJOML().add(lookDif)
                    val location: Vector3dc = Vector3d(s.shipGrabbedPos)
                    val position: Vector3dc = Vector3d(s.heldBlockPos)

                    val gravitronForceInducer = GravitronForceInducer.getOrCreate(ship)
                    val newData =
                        GravitronForceInducer.Companion.GravitronForceInducerData(position, rotation, location)
                    gravitronForceInducer.data = newData
                } else if (shipUnloaded == null) {
                    dropShip(s, level)
                }
            }
        }
    }

    private fun playerRotToQuaternion(pitch: Double, yaw: Double): Quaterniond {
        return Quaterniond().rotateY(toRadians(-yaw)).rotateX(toRadians(pitch))
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.NONE
    }

    override fun getArmPose(
        stack: ItemStack?,
        player: AbstractClientPlayer,
        hand: InteractionHand?
    ): HumanoidModel.ArmPose? {
        if (!player.swinging) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD
        }
        return null
    }

    override fun canAttackBlock(state: BlockState, world: Level, pos: BlockPos, player: Player): Boolean {
        return false
    }

    companion object {
        class GravitronState {
            var grabbing: Boolean = false
            var shouldDrop: Boolean = false
            var heldBlockPos: Vector3dc? = null
            var playerGrabbedRotation: Vector2dc? = null // Pitch , Yaw
            var shipGrabbedPos: Vector3dc? = null
            var shipGrabbedRot: Quaterniondc? = null
            var shipID: ShipId? = null
            var grabCD: Int? = 0
            var shipGrabbedDistance: Double? = null
        }
    }
}
