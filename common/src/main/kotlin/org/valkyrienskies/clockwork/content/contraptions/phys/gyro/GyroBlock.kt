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
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterScreen
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

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.isShiftKeyDown) {
            if (level.isClientSide) {
                withBlockEntityDo(level, pos) { te: GyroBlockEntity ->
                    displayScreen(te, player)
                }
            }

        } else {
            withBlockEntityDo(level, pos) { te: GyroBlockEntity ->
                //TODO test code
                if (te.targetVec3 == Vec3(0.0,1.0,0.0)) {
                    te.targetVec3 = Vec3(0.0,0.0,1.0)
                } else {
                    te.targetVec3 = Vec3(0.0,1.0,0.0)
                }
                te.notifyUpdate()
            }
        }
        return InteractionResult.SUCCESS
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

    @Environment(value = EnvType.CLIENT)
    private fun displayScreen(te: GyroBlockEntity, player: Player) {
        if (player is LocalPlayer) ScreenOpener.open(GyroScreen(te))
    }
}