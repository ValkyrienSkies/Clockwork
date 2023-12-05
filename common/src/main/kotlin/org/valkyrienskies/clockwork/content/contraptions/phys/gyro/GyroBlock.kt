package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos

class GyroBlock (properties: Properties) : Block(properties), IBE<GyroBlockEntity> {
    init {
        registerDefaultState(stateDefinition.any())
    }

    override fun getBlockEntityClass(): Class<GyroBlockEntity> {
        return GyroBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GyroBlockEntity> {
        return ClockworkBlockEntities.GYRO.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level.isClientSide) return
        level as ServerLevel

        val ship = level.getShipObjectManagingPos(pos) ?: level.getShipManagingPos(pos) ?: return
        GyroShipControl.getOrCreate(ship).gyros += 1
    }

    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        super.destroy(level, pos, state)

        if (level.isClientSide) return
        level as ServerLevel

        level.getShipManagingPos(pos)?.getAttachment<GyroShipControl>()?.let { control ->
            control.gyros -= 1
        }
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T> = BlockEntityTicker { level, pos, state, blockEntity ->
        if (level.isClientSide) return@BlockEntityTicker
        if (blockEntity is GyroBlockEntity) {
            blockEntity.tick()
        }
    }
}