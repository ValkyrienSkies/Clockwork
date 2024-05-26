package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import org.valkyrienskies.mod.common.util.toJOMLD

class GooBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    private fun bounce(level: Level, pos: BlockPos) {
        val shipMountedTo = level.getShipManagingPos(pos)

        level.transformToNearbyShipsAndWorld(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.05) { x, y, z ->
            val newBlockPos = BlockPos(x, y, z)
            val newSearchArea: AABB = AABB(newBlockPos).inflate(1.05)
            val shipTarget = level.getShipManagingPos(newBlockPos)
            val shipCenter = Vector3d(0.0, 0.0, 0.0)
            if (shipTarget != null && shipMountedTo != shipTarget) {
                var shipVelocity = shipTarget.velocity as Vector3d
                if (shipVelocity.lengthSquared() > 100) {
                    shipTarget.shipAABB!!.center(shipCenter)
                    val shipToBlock = shipCenter.sub(pos.toJOMLD())
                    shipToBlock.normalize()
                    shipVelocity.normalize()
                    if (shipToBlock.dot(shipVelocity) < 1) {
                        if (!this.level!!.getBlockStates(newSearchArea)
                                .allMatch { blockState.isAir }
                        ) {
                            println("FUCK I LOVE BOUNCING!")
                        }
                    }
                }
            }
        }

    }


    override fun tick() {
        val level = this.level!!
        if (level.isClientSide()) {
            return
        }
        if (this.blockPos != null) {
            bounce(level, this.blockPos)
        }
        val l = level.gameTime
        if (l % 20L == 0L) {
        }
    }
}