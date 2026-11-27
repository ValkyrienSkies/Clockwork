package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.content.logistics.gas.MeteredGasInlet
import org.valkyrienskies.kelvin.util.IEdgeBlock
import org.valkyrienskies.kelvin.util.INodeBlock

/**
 * Placement helper for duct-connected blocks, analogous to Create's preferred facing/axis
 * helpers for shafted kinetic blocks (`DirectionalKineticBlock.getPreferredFacing`,
 * `RotatedPillarKineticBlock.getPreferredAxis`, `HorizontalKineticBlock.getPreferredHorizontalFacing`).
 */
object DuctPlacementHelper {

    /**
     * Returns whether the block at [pos] can form a duct connection through its [face] side.
     */
    @JvmStatic
    fun hasDuctTowards(level: BlockGetter, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        val block = state.block
        if (block is DuctBlock) {
            return DuctBlock.connectInDirection(level, pos, state, face)
        }
        if (block is MeteredGasInlet && block.isMeteredInlet(state, face)) {
            return true
        }
        if (block is INodeBlock) {
            return block.canConnectTo(pos, pos.relative(face), face, level)
        }
        if (block is IEdgeBlock && level is Level) {
            return block.canConnectTo(level, pos, pos.relative(face))
        }
        return false
    }

    /**
     * Returns whether the block at [pos] actively initiates Kelvin duct edges with adjacent nodes.
     *
     * Passive [INodeBlock]s (such as thrusters, hose ports, or extendons) do not create edges on
     * their own, so they should only snap to neighbors that will actually establish an edge.
     */
    @JvmStatic
    fun isActiveConnector(level: BlockGetter, pos: BlockPos, state: BlockState): Boolean {
        val block = state.block
        return block is IDuct || block is IEdgeBlock || level.getBlockEntity(pos) is IConnectable
    }

    /**
     * Returns whether the neighbor at `pos.relative(direction)` has a duct connection facing back
     * toward [pos].
     */
    @JvmStatic
    @JvmOverloads
    fun prefersConnectionTo(
        level: BlockGetter,
        pos: BlockPos,
        direction: Direction,
        requireActiveConnector: Boolean = false
    ): Boolean {
        val neighborPos = pos.relative(direction)
        val neighborState = level.getBlockState(neighborPos)
        if (requireActiveConnector && !isActiveConnector(level, neighborPos, neighborState)) {
            return false
        }
        return hasDuctTowards(level, neighborPos, neighborState, direction.opposite)
    }

    /**
     * Returns the direction from `context.clickedPos` toward a preferred adjacent duct connection,
     * or `null` if no adjacent duct exists or perpendicular neighbors are ambiguous.
     */
    @JvmStatic
    @JvmOverloads
    fun getPreferredFacing(
        context: BlockPlaceContext,
        requireActiveConnector: Boolean = false
    ): Direction? {
        return getPreferredFacing(context, Iterate.directions, requireActiveConnector)
    }

    /**
     * Horizontal-only variant of [getPreferredFacing] for `HorizontalDirectionalBlock`s whose duct
     * ports lie along `HORIZONTAL_FACING`.
     */
    @JvmStatic
    @JvmOverloads
    fun getPreferredHorizontalFacing(
        context: BlockPlaceContext,
        requireActiveConnector: Boolean = false
    ): Direction? {
        return getPreferredFacing(context, Iterate.horizontalDirections, requireActiveConnector)
    }

    /**
     * Returns the preferred duct [Direction.Axis] at `context.clickedPos`, or `null` if no adjacent
     * duct exists or perpendicular neighbors are ambiguous.
     */
    @JvmStatic
    @JvmOverloads
    fun getPreferredAxis(
        context: BlockPlaceContext,
        requireActiveConnector: Boolean = false
    ): Direction.Axis? {
        return getPreferredFacing(context, requireActiveConnector)?.axis
    }

    /**
     * Convenience placement helper for [RotatedPillarBlock] duct blocks.
     *
     * Snaps `AXIS` to an adjacent duct unless the player is sneaking; falls back to [defaultAxis]
     * (or `context.clickedFace.axis` when sneaking next to a duct, matching Create's
     * `RotatedPillarKineticBlock`).
     */
    @JvmStatic
    @JvmOverloads
    fun getStateForPillarPlacement(
        defaultState: BlockState,
        context: BlockPlaceContext,
        defaultAxis: Direction.Axis = context.clickedFace.axis,
        requireActiveConnector: Boolean = false
    ): BlockState {
        val preferredAxis = getPreferredAxis(context, requireActiveConnector)
        val sneaking = context.player?.isShiftKeyDown == true
        val axis = resolvePillarAxis(
            preferredAxis = preferredAxis,
            clickedFaceAxis = context.clickedFace.axis,
            defaultAxis = defaultAxis,
            sneaking = sneaking
        )
        return defaultState.setValue(RotatedPillarBlock.AXIS, axis)
    }

    private fun getPreferredFacing(
        context: BlockPlaceContext,
        candidates: Array<Direction>,
        requireActiveConnector: Boolean
    ): Direction? {
        val level = context.level
        val pos = context.clickedPos
        val connected = candidates.filter { prefersConnectionTo(level, pos, it, requireActiveConnector) }
        return resolvePreferredDirection(
            connectedDirections = connected,
            clickedFace = context.clickedFace,
            nearestLookingDirections = context.nearestLookingDirections
        )
    }

    internal fun resolvePreferredDirection(
        connectedDirections: List<Direction>,
        clickedFace: Direction,
        nearestLookingDirections: Array<Direction>
    ): Direction? {
        if (connectedDirections.isEmpty()) return null
        if (connectedDirections.size == 1) return connectedDirections[0]

        val clickedNeighborSide = clickedFace.opposite
        val firstAxis = connectedDirections[0].axis
        val allSameAxis = connectedDirections.all { it.axis == firstAxis }

        if (allSameAxis) {
            if (clickedNeighborSide in connectedDirections) {
                return clickedNeighborSide
            }
            return nearestLookingDirections.firstOrNull { it in connectedDirections } ?: connectedDirections[0]
        }

        // Multiple perpendicular axes have adjacent ducts: prefer the duct the player explicitly
        // clicked on, otherwise treat it as ambiguous (matching Create's shaft placement helper).
        return if (clickedNeighborSide in connectedDirections) clickedNeighborSide else null
    }

    internal fun resolvePillarAxis(
        preferredAxis: Direction.Axis?,
        clickedFaceAxis: Direction.Axis,
        defaultAxis: Direction.Axis,
        sneaking: Boolean
    ): Direction.Axis {
        if (preferredAxis != null && !sneaking) return preferredAxis
        if (preferredAxis != null && sneaking) return clickedFaceAxis
        return defaultAxis
    }

    internal fun resolveDirectionalFacing(
        preferredSide: Direction?,
        nearestLookingDirection: Direction,
        sneaking: Boolean,
        portOnFacingOpposite: Boolean = true
    ): Direction {
        if (preferredSide != null && !sneaking) {
            return if (portOnFacingOpposite) preferredSide.opposite else preferredSide
        }
        return if (portOnFacingOpposite) {
            if (sneaking) nearestLookingDirection else nearestLookingDirection.opposite
        } else {
            if (sneaking) nearestLookingDirection.opposite else nearestLookingDirection
        }
    }

    internal fun resolveHorizontalFacing(
        preferredSide: Direction?,
        horizontalDirection: Direction,
        sneaking: Boolean
    ): Direction {
        if (preferredSide != null && !sneaking) {
            return preferredSide.opposite
        }
        return horizontalDirection.opposite
    }

    /**
     * Picks a `FACING` for a `DirectionalAxisKineticBlock` valve whose duct axis is perpendicular
     * to `FACING`. If [defaultFacing] lies on [preferredDuctAxis], shifts `FACING` onto a
     * perpendicular axis (avoiding any unique perpendicular shaft axis so both duct and shaft can
     * connect).
     */
    internal fun resolveValveFacing(
        defaultFacing: Direction,
        preferredDuctAxis: Direction.Axis?,
        perpendicularShaftDirections: List<Direction>,
        nearestLookingDirections: Array<Direction>
    ): Direction {
        if (preferredDuctAxis == null || defaultFacing.axis != preferredDuctAxis) {
            return defaultFacing
        }

        val shaftAxes = perpendicularShaftDirections
            .map { it.axis }
            .filter { it != preferredDuctAxis }
            .distinct()

        val targetAxis = if (shaftAxes.size == 1) {
            Direction.Axis.entries.first { it != preferredDuctAxis && it != shaftAxes[0] }
        } else {
            null
        }

        val chosen = if (targetAxis != null) {
            nearestLookingDirections.firstOrNull { it.axis == targetAxis }?.opposite
        } else {
            nearestLookingDirections.firstOrNull { it.axis != preferredDuctAxis }?.opposite
        }

        return chosen ?: defaultFacing
    }

    /**
     * Picks a `FACING` perpendicular to [preferredDuctAxis] for blocks whose duct ports lie on the
     * four sides perpendicular to `FACING` (such as `AirCompressorBlock`, where `FACING` is an air
     * intake and `FACING.opposite` is a kinetic shaft).
     *
     * Prefers candidates satisfying [preferDirection] (e.g. open air at the intake) in player look
     * order.
     */
    internal fun resolvePerpendicularFacing(
        defaultFacing: Direction,
        preferredDuctAxis: Direction.Axis?,
        nearestLookingDirections: Array<Direction>,
        preferDirection: (Direction) -> Boolean = { true }
    ): Direction {
        if (preferredDuctAxis == null || defaultFacing.axis != preferredDuctAxis) {
            return defaultFacing
        }
        val candidates = nearestLookingDirections
            .asSequence()
            .filter { it.axis != preferredDuctAxis }
            .map { it.opposite }
            .toList()
        return candidates.firstOrNull(preferDirection) ?: candidates.firstOrNull() ?: defaultFacing
    }
}
