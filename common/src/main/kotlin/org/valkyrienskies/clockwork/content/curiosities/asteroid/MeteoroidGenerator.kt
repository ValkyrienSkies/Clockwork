package org.valkyrienskies.clockwork.content.curiosities.asteroid

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource
import net.minecraft.world.level.levelgen.synth.PerlinNoise
import org.valkyrienskies.clockwork.ClockworkBlocks
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt


object MeteoroidGenerator {

    val resolution = 0.25


    fun generate(level: ServerLevel,center: BlockPos, threshold: Double, maxDistance: Int, metaBallsCount: Int) {

        val noise = PerlinNoise.create(SingleThreadedRandomSource(level.seed), mutableListOf(1,1))

        val balls = mutableListOf<BlockPos>()

        balls.add(center)

        for (i in  2..metaBallsCount) {
            balls.add(randomizedPos(center, maxDistance))
        }


        iterate(balls, threshold) { x:Int, y:Int, z:Int ->

            val t = threshold  - noise.getValue(x.toDouble()*resolution,y.toDouble()*resolution,z.toDouble()*resolution)/500
            val mb = metaBall(balls, BlockPos(x,y,z))
            if (mb > 2*t)  {
                level.setBlockAndUpdate(BlockPos(x,y,z), ClockworkBlocks.WANDERLITE_NYX_ORE.defaultState)
            } else if (mb > t) {
                level.setBlockAndUpdate(BlockPos(x, y, z), ClockworkBlocks.NYX.defaultState)
            }
        }
    }

    private fun iterate(balls: List<BlockPos>, threshold: Double, func: (x: Int, y:Int, z:Int) -> Unit) {
        val res = largestAndSmallest(balls)
        val smallest = res.first
        val largest = res.second


        for (x in smallest.x-(2/threshold).toInt()..largest.x+(2/threshold).toInt()) {
            for (y in smallest.y-(2/threshold).toInt()..largest.y+(2/threshold).toInt()) {
                for (z in smallest.z-(2/threshold).toInt()..largest.z+(2/threshold).toInt()) {
                    func(x,y,z)
                }
            }
        }

    }

    private fun largestAndSmallest(balls: List<BlockPos>): Pair<BlockPos, BlockPos> {
        var smallest = BlockPos(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        var largest = BlockPos(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        balls.forEach {
            if (it.x > largest.x) largest = BlockPos(it.x, largest.y, largest.z)
            if (it.x < smallest.x) smallest = BlockPos(it.x, smallest.y, smallest.z)

            if (it.y > largest.y) largest = BlockPos(largest.x, it.y, largest.z)
            if (it.y < smallest.y) smallest = BlockPos(smallest.x, it.y, smallest.z)

            if (it.z > largest.z) largest = BlockPos(largest.x, largest.y, it.z)
            if (it.z < smallest.z) smallest = BlockPos(smallest.x, smallest.y, it.z)
        }
        return Pair(smallest,largest)
    }

    private fun randomizedPos(pos: BlockPos, maxDistance: Int): BlockPos {
        return pos.offset(Random.nextInt(-maxDistance..maxDistance),Random.nextInt(-maxDistance..maxDistance),Random.nextInt(-maxDistance..maxDistance))
    }

    private fun metaBall(balls: List<BlockPos>, pos: BlockPos): Double {
        var sigma = 0.0
        for (ball in balls) {
            sigma += 1/ball.distSqr(pos)
        }
        return sigma
    }


}