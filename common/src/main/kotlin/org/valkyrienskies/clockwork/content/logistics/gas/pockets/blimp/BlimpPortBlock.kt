package org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.ValveDuctNode

/**
 * Omnidirectional valve port embedded in a sealed blimp envelope wall.
 *
 * - [FACING]: points into the blimp interior (flush in the hull wall).
 * - [FACING].opposite: exterior Kelvin duct connection.
 * - [getRotationAxis]: kinetic shaft axis controlling valve opening (0..1).
 * - [getPointerAxis]: remaining perpendicular axis carrying the valve indicator and Thinkable ports.
 */
class BlimpPortBlock(properties: Properties) : DirectionalAxisKineticBlock(properties), IDuct, IBE<BlimpPortBlockEntity>, IHaveDuctStats {

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)

        val ductDir = state.getValue(FACING).opposite
        withBlockEntityDo(level, pos) {
            it.updateConnection(it.level!!, pos, ductDir)
            it.requestScan()
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide && (!state.`is`(newState.block) || !newState.hasBlockEntity())) {
            withBlockEntityDo(level, pos) { it.onPortRemoved() }
        }
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (player.getItemInHand(hand).isEmpty) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos) { it.requestScan() }
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        return super.use(state, level, pos, player, hand, hit)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)
        if (state.block !is BlimpPortBlock) return false
        if (direction != state.getValue(FACING).opposite) return false

        return super.canConnectTo(self, other, direction, level)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        movedByPiston: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)

        if (state.block !is BlimpPortBlock) return
        val direction = Direction.fromDelta(neighborPos.x - pos.x, neighborPos.y - pos.y, neighborPos.z - pos.z) ?: return
        val facing = state.getValue(FACING)
        if (direction == facing.opposite) {
            withBlockEntityDo(level, pos) { it.updateConnection(it.level!!, pos, direction) }
        } else if (direction == facing && !level.isClientSide) {
            withBlockEntityDo(level, pos) { it.requestScan() }
        }
    }

    override fun getBlockEntityClass(): Class<BlimpPortBlockEntity> = BlimpPortBlockEntity::class.java

    override fun getBlockEntityType(): BlockEntityType<out BlimpPortBlockEntity> = ClockworkBlockEntities.BLIMP_PORT.get()

    override fun createNode(pos: DuctNodePos): DuctNode {
        return ValveDuctNode(
            pos = pos,
            volume = getInternalVolume(),
            maxPressure = 16375049.0,
            maxTemperature = 1478.0,
            heatCapacity = 44.9,
            radius = 0.0,
        )
    }

    override fun getInternalVolume(): Double = ClockworkConfig.SERVER.ducts.ductVolme

    override fun getMaximumPressure(): Double = 0.0

    override fun getMaximumTemperature(): Double = 0.0

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(
            Component.translatable("vs_clockwork.blimp_port.function")
                .withStyle(ChatFormatting.GRAY)
                .withStyle(ChatFormatting.ITALIC)
        )
    }

    companion object {
        const val MAX_FLOW_RADIUS = 0.3125

        /** Returns the axis perpendicular to both [FACING] and the kinetic rotation axis. */
        fun getPointerAxis(state: BlockState): Axis {
            val block = state.block as? BlimpPortBlock
                ?: throw IllegalStateException("Provided BlockState is not a BlimpPortBlock.")
            val facingAxis = state.getValue(FACING).axis
            val shaftAxis = block.getRotationAxis(state)
            return Axis.values().first { it != facingAxis && it != shaftAxis }
        }

        fun getPointerFace(state: BlockState): Direction =
            Direction.fromAxisAndDirection(getPointerAxis(state), Direction.AxisDirection.POSITIVE)
    }
}
