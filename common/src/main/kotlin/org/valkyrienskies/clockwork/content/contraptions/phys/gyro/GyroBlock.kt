package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos

class GyroBlock(properties: Properties) : KineticBlock(properties), IBE<GyroBlockEntity> {
    init {
        registerDefaultState(stateDefinition.any())
    }

    override fun getBlockEntityClass(): Class<GyroBlockEntity> {
        return GyroBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GyroBlockEntity> {
        return ClockworkBlockEntities.GYRO.get()
    }

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter?,
        pPos: BlockPos?,
        pContext: CollisionContext?
    ): VoxelShape {
        return ClockworkShapes.GYRO
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

    override fun hasShaftTowards(world: LevelReader?, pos: BlockPos?, state: BlockState?, face: Direction): Boolean {
        return face == Direction.DOWN
    }

    override fun getRotationAxis(state: BlockState?): Direction.Axis {
        return Direction.Axis.Y
    }

    override fun getRenderShape(pState: BlockState?): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }
}