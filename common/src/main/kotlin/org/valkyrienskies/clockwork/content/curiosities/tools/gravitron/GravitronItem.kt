package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniondc
import org.joml.Vector2dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.SelectedAreaToolkit
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.function.Consumer

class GravitronItem(properties: Properties) : CWItem(properties), CustomArmPoseItem {

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        if (entity !is Player || level !is ServerLevel) {
            return
        }

        if (stack.`is`(ClockworkItems.GRAVITRON.get().asItem()) && !isSelected) {
            GrabTool.dropShip(entity)

            if (stack.tag != null) {
                if (stack.tag!!.contains("ShipId")) {
                    stack.tag!!.remove("ShipId")
                }
                if (stack.tag!!.contains("GrabbedPosInShip")) {
                    stack.tag!!.remove("GrabbedPosInShip")
                }
            }
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.NONE
    }

    override fun getArmPose(stack: ItemStack?, player: AbstractClientPlayer, hand: InteractionHand?): HumanoidModel.ArmPose? {
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
            var heldBlockPos: Vector3dc? = null
            var playerGrabbedRotation: Vector2dc? = null // Pitch , Yaw
            var shipGrabbedPos: Vector3dc? = null
            var shipGrabbedRot: Quaterniondc? = null
            var shipID: ShipId? = null
            var shipGrabbedDistance: Double? = null
        }

        /**
         * Given a SelectedAreaToolkit this function will try and assemble a ship, if grab is true it will also store
         * some nbt on the Gravitron to queue a grab in GrabTool#tick
         */
        fun abstractAssemble(level: Level, player: Player, toolkit: SelectedAreaToolkit, blockPos: BlockPos, clickLocation: Vec3, grab: Boolean): Boolean {
            toolkit.selectionClusters.forEach { cluster ->
                val selection: DenseBlockPosSet = SelectedAreaToolkit.denseBlocksFromCluster(cluster)

                if (selection.isEmpty() || !selection.contains(blockPos.x, blockPos.y, blockPos.z)) {
                    return@forEach
                }

                selection.forEach { x, y, z ->
                    if (level is ServerLevel) {
                        val serverLevel = level
                        if (!serverLevel.getBlockState(BlockPos(x, y, z)).isAir) {
                            val connectedShip = createNewShipWithBlocks(blockPos, selection, serverLevel)

                            val caughtEntities = SelectedAreaToolkit.entitiesFromCluster(cluster, serverLevel)
                            toolkit.dumpCluster(cluster)
                            if (caughtEntities.isNotEmpty()) {
                                caughtEntities.forEach(Consumer { entity: Entity ->
                                    if (entity is AbstractContraptionEntity || entity is SuperGlueEntity || entity is SeatEntity) {
                                        if (entity !is SuperGlueEntity) {
                                            val oldPos: Vector3dc = entity.position().toJOML()
                                            val newPos: Vector3dc = connectedShip.transform.worldToShip.transformPosition(oldPos, Vector3d())
                                            entity.moveTo(newPos.toMinecraft())
                                        } else {
                                            val oldBounds = entity.boundingBox
                                            val oldMax: Vector3dc = Vector3d(oldBounds.maxX, oldBounds.maxY, oldBounds.maxZ)
                                            val oldMin: Vector3dc = Vector3d(oldBounds.minX, oldBounds.minY, oldBounds.minZ)
                                            val newMax: Vector3dc = connectedShip.transform.worldToShip.transformPosition(oldMax, Vector3d())
                                            val newMin: Vector3dc = connectedShip.transform.worldToShip.transformPosition(oldMin, Vector3d())
                                            val newBounds = AABB(newMin.x(), newMin.y(), newMin.z(), newMax.x(), newMax.y(), newMax.z())
                                            entity.boundingBox = newBounds
                                            entity.resetPositionToBB()
                                        }
                                    }
                                })
                            }
                            if (grab) {
                                val grabPosInShip: Vec3 = clickLocation
                                val tag = player.mainHandItem.orCreateTag
                                tag.putLong("ShipId", connectedShip.id)
                                tag.put("GrabbedPosInShip", ClockworkUtils.writeVec3(grabPosInShip))
                            }

                            return true
                        }
                    }
                }
            }
            return false
        }

        /**
         * Checking players inventory for an Auric Designator, extracts the first founds SelectedAreaToolkit
         * to try and assemble the ship, if the player already has a ship connected to the Gravitron, don't proceed with the assembly
         */
        fun grabssemble(level: Level, player: Player, blockPos: BlockPos, clickLocation: Vec3, grab: Boolean): Boolean {
            if (getState(player).shipID != null) {
                return false
            }

            for (item in player.inventory.items) {
                if (item.`is`(ClockworkItems.WANDERWAND.get().asItem())) {
                    val auricItem: WanderWandItem = item.item as WanderWandItem

                    if (abstractAssemble(level, player, auricItem.selectedArea, blockPos, clickLocation, grab)) {
                        return true;
                    }
                    break
                }
            }

            return false
        }

        /**
         * Will freeze or unfreeze a ship in its position
         */
        @JvmStatic
        fun leftClickItem(player: Player, state: GravitronState): Boolean {
            val level = player.level
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
    }
}
