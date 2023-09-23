package org.valkyrienskies.clockwork.content.physicalities.wing

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.clockwork.util.blocktype.DyedWing
import org.valkyrienskies.core.api.ships.Wing
import org.valkyrienskies.mod.common.block.WingBlock

class FlapBlock(properties: Properties?) : DyedWing(properties),
    WingBlock {
    override fun getNewState(state: BlockState?, level: Level?, pos: BlockPos?): BlockState? {
        val facing = state!!.getValue(FACING)
        val north = level!!.getBlockState(pos!!.north())
        val south = level.getBlockState(pos.south())
        val east = level.getBlockState(pos.east())
        val west = level.getBlockState(pos.west())
        val up = level.getBlockState(pos.above())
        val down = level.getBlockState(pos.below())
        return when (facing) {
            Direction.NORTH, Direction.SOUTH -> state
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(
                    EAST,
                    east.block is FlapBlock && east.getValue(FACING) == facing
                )
                .setValue(
                    WEST,
                    west.block is FlapBlock && west.getValue(FACING) == facing
                )
                .setValue(
                    UP,
                    up.block is FlapBlock && up.getValue(FACING) == facing
                )
                .setValue(
                    DOWN,
                    down.block is FlapBlock && down.getValue(FACING) == facing
                )
                .setValue(FACING, Direction.NORTH)

            Direction.EAST, Direction.WEST -> state
                .setValue(
                    NORTH,
                    north.block is FlapBlock && north.getValue(FACING) == facing
                )
                .setValue(
                    SOUTH,
                    south.block is FlapBlock && south.getValue(FACING) == facing
                )
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(
                    UP,
                    up.block is FlapBlock && up.getValue(FACING) == facing
                )
                .setValue(
                    DOWN,
                    down.block is FlapBlock && down.getValue(FACING) == facing
                )
                .setValue(FACING, Direction.EAST)

            Direction.UP, Direction.DOWN -> state
                .setValue(
                    NORTH,
                    north.block is FlapBlock && north.getValue(FACING) == facing
                )
                .setValue(
                    SOUTH,
                    south.block is FlapBlock && south.getValue(FACING) == facing
                )
                .setValue(
                    EAST,
                    east.block is FlapBlock && east.getValue(FACING) == facing
                )
                .setValue(
                    WEST,
                    west.block is FlapBlock && west.getValue(FACING) == facing
                )
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(FACING, Direction.UP)
        }
    }

    override fun getWing(level: Level?, pos: BlockPos?, blockState: BlockState): Wing {
        val wingPower = 150.0
        val wingDrag = 150.0
        val wingBreakingForce = 10.0
        return when (blockState.getValue(FACING)) {
            Direction.EAST, Direction.WEST -> Wing(Vector3d(1.0, 0.0, 0.0), wingPower, wingDrag, wingBreakingForce, 0.0)
            Direction.UP, Direction.DOWN -> Wing(Vector3d(0.0, 1.0, 0.0), wingPower, wingDrag, wingBreakingForce, 0.0)
            Direction.NORTH, Direction.SOUTH -> Wing(
                Vector3d(0.0, 0.0, 1.0),
                wingPower,
                wingDrag,
                wingBreakingForce,
                0.0
            )
        }
    }
}