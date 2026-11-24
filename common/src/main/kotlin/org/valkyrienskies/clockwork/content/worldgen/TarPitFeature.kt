package org.valkyrienskies.clockwork.content.worldgen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration
import net.minecraft.world.level.levelgen.synth.PerlinNoise
import org.valkyrienskies.clockwork.ClockworkMod
import kotlin.math.roundToInt

class TarPitFeature : Feature<BlockStateConfiguration>(BlockStateConfiguration.CODEC) {
    override fun place(context: FeaturePlaceContext<BlockStateConfiguration>): Boolean {
        val level = context.level()
        val origin = context.origin()
        val random = context.random()
        val radiusX = 2.5 + (random.nextDouble() - 0.5) * 0.6
        val radiusZ = 2.5 + (random.nextDouble() - 0.5) * 0.6
        val depth = 3.0 + (random.nextDouble() - 0.5) * 0.8
        val noise = PerlinNoise.create(RandomSource.create(random.nextLong()), listOf(0))
        val columns = mutableMapOf<BlockPos, Int>()

        for (x in -3..3) {
            for (z in -3..3) {
                val distance = x * x / (radiusX * radiusX) + z * z / (radiusZ * radiusZ)
                val distortion = noise.getValue(x * 0.5, 0.0, z * 0.5) * 0.6
                val columnDepth = (depth * (1.0 - distance) + distortion).roundToInt()
                if (columnDepth > 0) columns[BlockPos(origin.x + x, 0, origin.z + z)] = columnDepth
            }
        }

        val rim = columns.keys.toMutableSet()
        for (pos in columns.keys) {
            for (direction in Direction.Plane.HORIZONTAL) rim.add(pos.relative(direction))
        }

        val heights = mutableMapOf<BlockPos, Int>()
        for (pos in rim) {
            val surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.x, pos.z) - 1
            val ground = BlockPos(pos.x, surface, pos.z)
            val state = level.getBlockState(ground)
            val above = level.getBlockState(ground.above())
            if (!state.`is`(REPLACEABLE) || !state.fluidState.isEmpty || state.hasBlockEntity()) return false
            if (!state.isFaceSturdy(level, ground, Direction.UP)) return false
            if (!above.fluidState.isEmpty || (!above.isAir && !above.canBeReplaced())) return false
            heights[pos] = surface
        }

        val surface = heights.values.min()
        if (heights.values.max() - surface > 1) return false
        val cavity = mutableSetOf<BlockPos>()
        for ((pos, columnDepth) in columns) {
            for (y in surface - columnDepth + 1..surface) cavity.add(BlockPos(pos.x, y, pos.z))
        }

        for (pos in cavity) {
            if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false
            val state = level.getBlockState(pos)
            if (!state.`is`(REPLACEABLE) || !state.fluidState.isEmpty || state.hasBlockEntity()) return false
            for (direction in Direction.values()) {
                if (direction == Direction.UP) continue
                val neighbor = pos.relative(direction)
                if (neighbor in cavity) continue
                val wall = level.getBlockState(neighbor)
                if (!wall.fluidState.isEmpty || !wall.isFaceSturdy(level, neighbor, direction.opposite)) return false
            }
        }

        for (pos in columns.keys) {
            for (y in surface + 1..heights.getValue(pos) + 1) {
                val above = BlockPos(pos.x, y, pos.z)
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 2)
                markAboveForPostProcessing(level, above)
            }
        }
        for (pos in cavity) level.setBlock(pos, context.config().state, 2)
        return true
    }

    companion object {
        private val REPLACEABLE: TagKey<Block> = TagKey.create(Registries.BLOCK, ClockworkMod.asResource("tar_pit_replaceable"))
    }
}
