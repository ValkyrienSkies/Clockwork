package org.valkyrienskies.clockwork.content.curiosities

import com.simibubi.create.content.materials.ExperienceBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft

class WanderliteOreBlock(properties: Properties) : ExperienceBlock(properties), IWanderliteBlock {

    override fun use(state: BlockState,
                     level: Level,
                     pos: BlockPos,
                     player: Player,
                     hand: InteractionHand,
                     hit: BlockHitResult): InteractionResult {

        if (level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.FAIL;
    }

    override fun attack(state: BlockState, level: Level, pos: BlockPos, player: Player) {
        if (level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
        }
        super.attack(state, level, pos, player)
    }

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        if (entity is Player && !entity.isSteppingCarefully && level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
        }

        super.stepOn(level, pos, state, entity)
    }

    fun isAlreadyShip(level: ServerLevel, blockPos: BlockPos): Boolean {
        return level.isBlockInShipyard(blockPos.x, blockPos.y, blockPos.z)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (level is ServerLevel) {
            val ship = level.getShipObjectManagingPos(pos)
            if (ship != null) {
                addToShip(level, ship, pos)
            }
        }
        super.onPlace(state, level, pos, oldState, movedByPiston)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level is ServerLevel) {
            val ship = level.getShipObjectManagingPos(pos)
            if (ship != null) {
                removeFromShip(ship, pos)
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        if (level is ServerLevel) {
            val ship = level.getShipObjectManagingPos(pos)
            if (ship != null) {
                removeFromShip(ship, pos)
            }
        }
        super.destroy(level, pos, state)
    }
}