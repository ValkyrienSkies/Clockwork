package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.utility.VoxelShaper
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.function.BiFunction

object ClockworkShapes {
    val WING = shape(0.0, 4.0, 0.0, 16.0, 12.0, 16.0).forAxis()
    val AFTERBLAZER = shape(1.0, 0.0, 1.0, 15.0, 14.0, 15.0).forDirectional()
    val ALT_METER = shape(1.0, 0.0, 1.0, 15.0, 5.0, 15.0).add(4.0, 8.0, 4.0, 12.0, 16.0, 12.0).build()

    val GYRO = shape(1.0, 0.0, 1.0, 15.0, 5.0, 15.0)
        .add(5.0, 5.0, 5.0, 11.0, 10.0, 11.0).build()

    private fun shape(shape: VoxelShape): Builder {
        return Builder(shape)
    }

    private fun shape(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Builder {
        return shape(cuboid(x1, y1, z1, x2, y2, z2))
    }

    private fun cuboid(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): VoxelShape {
        return Block.box(x1, y1, z1, x2, y2, z2)
    }

    class Builder(private var shape: VoxelShape) {
        fun add(shape: VoxelShape?): Builder {
            this.shape = Shapes.or(this.shape, shape)
            return this
        }

        fun add(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Builder {
            return add(cuboid(x1, y1, z1, x2, y2, z2))
        }

        fun erase(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Builder {
            shape = Shapes.join(shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST)
            return this
        }

        fun build(): VoxelShape {
            return shape
        }

        fun build(factory: BiFunction<VoxelShape?, Direction?, VoxelShaper>, direction: Direction?): VoxelShaper {
            return factory.apply(shape, direction)
        }

        fun build(factory: BiFunction<VoxelShape?, Direction.Axis?, VoxelShaper>, axis: Direction.Axis?): VoxelShaper {
            return factory.apply(shape, axis)
        }

        @JvmOverloads
        fun forDirectional(direction: Direction? = Direction.UP): VoxelShaper {
            return build({ shape: VoxelShape?, facing: Direction? ->
                VoxelShaper.forDirectional(
                    shape,
                    facing
                )
            }, direction)
        }

        fun forAxis(): VoxelShaper {
            return build({ shape: VoxelShape?, along: Direction.Axis? ->
                VoxelShaper.forAxis(
                    shape,
                    along
                )
            }, Direction.Axis.Y)
        }

        fun forHorizontalAxis(): VoxelShaper {
            return build({ shape: VoxelShape?, along: Direction.Axis? ->
                VoxelShaper.forHorizontalAxis(
                    shape,
                    along
                )
            }, Direction.Axis.Z)
        }

        fun forHorizontal(direction: Direction?): VoxelShaper {
            return build({ shape: VoxelShape?, facing: Direction? ->
                VoxelShaper.forHorizontal(
                    shape,
                    facing
                )
            }, direction)
        }
    }
}