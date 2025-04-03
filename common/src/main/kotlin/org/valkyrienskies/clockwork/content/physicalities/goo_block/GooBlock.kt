package org.valkyrienskies.clockwork.content.physicalities.goo_block

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState.Companion.getState
import org.valkyrienskies.clockwork.content.forces.GooController
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.api.getShipManagingBlock
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.*

class GooBlock(properties: Properties) : Block(properties.noOcclusion()), IBE<GooBlockEntity> {
    override fun getBlockEntityClass(): Class<GooBlockEntity> {
        return GooBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GooBlockEntity> {
        return ClockworkBlockEntities.GOO_BLOCK.get()

    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1)
        }
        super.onPlace(state, level, pos, oldState, isMoving)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random) {

        val ship = level.getShipManagingBlock(pos)
        for (direction in Direction.entries) {
            if (!level.getBlockState(pos.offset(direction.normal)).isAir) continue
            val vec3Pos = pos.toDoubles().add(0.5,0.5,0.5)

            val context = ClipContext(vec3Pos.add(direction.normal.toDoubles().scale(0.75)), vec3Pos.add(direction.normal.toDoubles()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null)
            val clip = level.clipIncludeShips(context, false, ship?.id)
            if (clip.type == HitResult.Type.MISS) continue
            val clipShip = level.getShipManagingBlock(clip.blockPos) ?: continue
            // Have to do this, since type cast to loaded ship is impossible
            val loadedClipShip = level.shipObjectWorld.loadedShips.getById(clipShip.id) ?: continue
            GooController.getOrCreate(loadedClipShip)?.addCollision(pos)
            if (ship != null) GooController.getOrCreate(loadedClipShip)?.addCollision(clip.blockPos)
        }

        level.scheduleTick(pos, this, 1)
    }
}