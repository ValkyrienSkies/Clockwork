package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.foundation.item.CustomArmPoseItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
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
import org.valkyrienskies.clockwork.AreaData
import org.valkyrienskies.clockwork.ClockworkSounds
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

    //TODO implement this with new system
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



    // || ITEM FUNCTIONS || //
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val s: GravitronState = getState(player)
        if ((s.shipID != null) && (s.grabCD == 0) && s.grabbing) {
            s.shouldDrop = true
        }
        return super.use(level, player, usedHand)
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
