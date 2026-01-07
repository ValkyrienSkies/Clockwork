package org.valkyrienskies.clockwork.content.curiosities.debug

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.curiosities.IArcConnector
import org.valkyrienskies.mod.common.toWorldCoordinates

class DebugLightningArcerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
), IArcConnector {
    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {

    }

    override fun initialize() {
        super.initialize()
        if (this.level?.isClientSide == true) {
            ClockworkModClient.LIGHTNING_NODES[worldPosition] = (this)
        }
    }

    override fun remove() {
        if (this.level?.isClientSide == true) {
            ClockworkModClient.LIGHTNING_NODES.remove(worldPosition)
        }
        super.remove()
    }

    override fun canConnect(other: IArcConnector): Boolean {
        return true
    }

    override fun getMaxConnections(): Int {
        return 0
    }

    override fun getWorldPos(): Vec3 {
        return this.level.toWorldCoordinates(Vec3.atCenterOf(worldPosition))
    }
}
