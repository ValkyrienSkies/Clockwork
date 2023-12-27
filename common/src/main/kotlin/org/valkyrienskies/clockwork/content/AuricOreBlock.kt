package org.valkyrienskies.clockwork.content

import com.simibubi.create.content.materials.ExperienceBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.yRange
import org.valkyrienskies.mod.util.relocateBlock

class AuricOreBlock(properties: Properties) : ExperienceBlock(properties) {

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

    override fun attack(state: BlockState?, level: Level?, pos: BlockPos, player: Player?) {
        if (level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
        }
        //super.attack(state, level, pos, player)
    }

    override fun stepOn(level: Level?, pos: BlockPos, state: BlockState?, entity: Entity) {
        if (!entity.isSteppingCarefully && level is ServerLevel && !isAlreadyShip(level, pos)) {
            shipifyBlock(level, pos)
        }

        super.stepOn(level, pos, state, entity)
    }

    fun isAlreadyShip(level: ServerLevel, blockPos: BlockPos): Boolean {
        return level.isBlockInShipyard(blockPos.x, blockPos.y, blockPos.z)
    }

    fun shipifyBlock(level: ServerLevel, blockPos: BlockPos)  {
        val dimensionId = level.dimensionId
        val serverShip = level.shipObjectWorld.createNewShipAtBlock(blockPos.toJOML(), false, 1.0, dimensionId)
        val centerPos = serverShip.chunkClaim.getCenterBlockCoordinates(level.yRange).toBlockPos()
        level.relocateBlock(blockPos, centerPos, true, serverShip, Rotation.NONE)
        AuricShipControl.getOrCreate(serverShip)
    }
}