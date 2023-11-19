package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
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
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector2d
import org.joml.Vector2dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit
import org.valkyrienskies.clockwork.AreaData
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.item.ShipAssemblerItem
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import java.lang.Math.toRadians
import java.util.function.Consumer

class GravitronItem(properties: Properties) : CWItem(properties), CustomArmPoseItem {
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
        if (level is ServerLevel && player != null) {
            val s: GravitronState = getState(player)
            if ((s.shipID == null) && !player.cooldowns.isOnCooldown(this) && !s.grabbing) {
                s.grabbing = true
                player.cooldowns.addCooldown(this, 20)
                s.grabCD = 20
                tryGrabShip(s, level, context)
            }
        }
        return super.useOn(context)
    }

    //Sterner test code
    fun assemble(serverLevel : ServerLevel, player: Player, blockPos: BlockPos): LoadedServerShip? {
        val data = AreaData.of(player).get()
        val list = data.area
        var ship: LoadedServerShip? = null
        list.selectionClusters.forEach{cluster ->
            val selection: DenseBlockPosSet = SelectedAreaToolkit.denseBlocksFromCluster(cluster)
            val connectedShip = createNewShipWithBlocks(blockPos, selection, serverLevel)
            val caughtEntities: Set<Entity> = SelectedAreaToolkit.entitiesFromCluster(cluster, serverLevel)
            caughtEntities.forEach(Consumer { entity: Entity ->
                if (entity is AbstractContraptionEntity || entity is SuperGlueEntity || entity is SeatEntity) {
                    if (entity !is SuperGlueEntity) {
                        val oldPos: Vector3dc = entity.position().toJOML()
                        val newPos: Vector3dc = connectedShip.transform.worldToShip.transformPosition(oldPos, Vector3d())
                        entity.moveTo(newPos.toMinecraft())
                    } else {
                        val glueEntity = entity
                        val oldBounds = glueEntity.boundingBox
                        val oldMax: Vector3dc = Vector3d(oldBounds.maxX, oldBounds.maxY, oldBounds.maxZ)
                        val oldMin: Vector3dc = Vector3d(oldBounds.minX, oldBounds.minY, oldBounds.minZ)
                        val newMax: Vector3dc = connectedShip.transform.worldToShip.transformPosition(oldMax, Vector3d())
                        val newMin: Vector3dc = connectedShip.transform.worldToShip.transformPosition(oldMin, Vector3d())
                        val newBounds = AABB(
                            newMin.x(),
                            newMin.y(),
                            newMin.z(),
                            newMax.x(),
                            newMax.y(),
                            newMax.z()
                        )
                        glueEntity.boundingBox = newBounds
                        glueEntity.resetPositionToBB()
                    }
                }
            })
            if (connectedShip is LoadedServerShip) {
                ship = connectedShip
            }

            serverLevel.playLocalSound(blockPos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1f, false)
        }
        data.removeArea(list)

        return ship;
    }

    // || ITEM FUNCTIONS || //
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val s: GravitronState = getState(player)
        if ((s.shipID != null) && (s.grabCD == 0) && s.grabbing) {
            s.shouldDrop = true
        }
        return super.use(level, player, usedHand)
    }

    override fun inventoryTick(
        stack: ItemStack,
        level: Level,
        entity: Entity,
        slotId: Int,
        isSelected: Boolean
    ) {
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
        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    // called first to put the ship into the players grasp
    private fun tryGrabShip(
        s: GravitronState,
        level: ServerLevel,
        context: UseOnContext
    ) {
        val player = context.player ?: return
        val chunkX = context.clickedPos.x shr 4
        val chunkZ = context.clickedPos.z shr 4
        var ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getByChunkPos(chunkX, chunkZ, level.dimensionId)
        val grabPosInShip: Vector3dc = context.clickLocation.toJOML()
        val grabPosInWorld = Vector3d(grabPosInShip)
        if (level.isBlockInShipyard(context.clickedPos) && ship == null) {
            return
        }
        if (ship == null) {
            ship = assemble(level, player, context.clickedPos)
        } else {
            ship.shipToWorld.transformPosition(grabPosInWorld)
            grabShip(s, player, ship, grabPosInShip)
        }

        if (ship == null) {
            s.grabbing = false;
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
