package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
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
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector2d
import org.joml.Vector2dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import java.lang.Math.toRadians

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
        val ship: LoadedServerShip? = level.shipObjectWorld.loadedShips.getByChunkPos(chunkX, chunkZ, level.dimensionId)
        val grabPosInShip: Vector3dc = context.clickLocation.toJOML()
        val grabPosInWorld = Vector3d(grabPosInShip)
        if (level.isBlockInShipyard(context.clickedPos) && ship == null) {
            return
        }
        if (ship == null) {
            return  // todo: try to assemble a ship when grabbing
            //            DenseBlockPosSet toAssemble = new DenseBlockPosSet();
//            toAssemble.add(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ());
//            ship = ShipAssemblyKt.createNewShipWithBlocks(context.getClickedPos(), toAssemble, level);
//            ship.getWorldToShip().transformPosition(grabPosInShip);
        } else {
            ship.shipToWorld.transformPosition(grabPosInWorld)
        }
        grabShip(s, player, ship, grabPosInShip)
    }

    // || SHIP FUNCTIONS || //
    private fun grabShip(
        s: GravitronState,
        p: Player,
        ship: LoadedServerShip,
        grabPosInShip: Vector3dc
    ) {
        s.shipID = ship.id
        s.heldBlockPos = ship.transform.shipToWorld.transformPosition(Vector3d(grabPosInShip))
        s.playerGrabbedRotation = Vector2d(p.xRot.toDouble(), p.yRot.toDouble())
        s.shipGrabbedPos = Vector3d(grabPosInShip)
        s.shipGrabbedRot = ship.transform.shipToWorldRotation
    }

    // sets down the ship
    private fun dropShip(s: GravitronState, level: ServerLevel) {
        val grabbedShipId = s.shipID
        if (grabbedShipId != null) {
            val loadedShip = level.shipObjectWorld.loadedShips.getById(grabbedShipId)
            if (loadedShip != null) {
                val gravitronForceInducer = GravitronForceInducer.getOrCreate(loadedShip)
                gravitronForceInducer.idealPos = null
                gravitronForceInducer.idealRot = null
            }
        }

        s.grabbing = false
        s.shipID = null
        s.positionConstraintID = null
        s.rotationConstraintID = null
        s.positionDampeningConstraintID = null
        s.rotationDampeningConstraintID = null
        s.shouldDrop = false
    }

    // ONLY IN DEBUG SHOULD THIS BE USED
    fun printRemovedConstraints(vararg constraints: Int?) {
        for (constraint: Int? in constraints) {
            if (constraint != null) {
//                System.out.println("Removed " + constraint);
            }
        }
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
                val worldShipID: ShipId =
                    level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
                if (ship != null) {
                    // Update Rot Values
                    val playerCurrentRotation: Vector2dc = Vector2d(entity.xRot.toDouble(), entity.yRot.toDouble())
                    val origPlayerRot: Quaterniondc = playerRotToQuaternion(s.playerGrabbedRotation!!.x(), s.playerGrabbedRotation!!.y()).normalize()
                    val newPlayerRot: Quaterniondc = playerRotToQuaternion(playerCurrentRotation.x(), playerCurrentRotation.y()).normalize()
                    val deltaPlayerRot: Quaterniondc = newPlayerRot.mul(origPlayerRot.conjugate(Quaterniond()), Quaterniond())
                    val rotation: Quaterniondc = deltaPlayerRot.mul(s.shipGrabbedRot, Quaterniond()).normalize()

                    // Update Pos Values
                    s.heldBlockPos = entity.position().toJOML().add(0.0, entity.eyeHeight.toDouble(), 0.0).add(entity.lookAngle.toJOML().normalize().mul(getShipSize(ship)))
                    val location: Vector3dc = Vector3d(s.shipGrabbedPos)
                    val position: Vector3dc = Vector3d(s.heldBlockPos)

                    val gravitronForceInducer = GravitronForceInducer.getOrCreate(ship)
                    gravitronForceInducer.idealPos = position // TODO: This isnt quite correct, but whatever, use location to fix this
                    gravitronForceInducer.idealRot = rotation
                } else if (shipUnloaded == null) {
                    dropShip(s, level)
                }
            }
        }
    }

    // || MATH FUNCTIONS || //
    private fun getShipSize(ship: Ship?): Double {
        val aabb = ship?.shipAABB
        return if (aabb != null) {
            val minVector = Vector3d(
                aabb.minX().toDouble(),
                aabb.minY().toDouble(),
                aabb.minZ().toDouble()
            )
            val maxVector = Vector3d(
                aabb.maxX().toDouble(),
                aabb.maxY().toDouble(),
                aabb.maxZ().toDouble()
            )
            minVector.sub(maxVector).length() + 0.75
        } else 0.0
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
            var positionConstraintID: Int? = null
            var rotationConstraintID: Int? = null
            var positionDampeningConstraintID: Int? = null
            var rotationDampeningConstraintID: Int? = null
            var grabCD: Int? = 0
        }
    }
}
