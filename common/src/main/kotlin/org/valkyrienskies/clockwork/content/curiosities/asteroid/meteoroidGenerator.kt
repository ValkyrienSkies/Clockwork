package org.valkyrienskies.clockwork.content.curiosities.asteroid

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.loader.impl.lib.sat4j.core.Vec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.RandomSource
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource
import net.minecraft.world.level.levelgen.synth.PerlinNoise
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.random.nextInt


object meteoroidGenerator {

    fun generate(level: ServerLevel,center: BlockPos, threshold: Double, maxDistance: Int) {



        val center_2 = center.offset(Random.nextInt(-maxDistance..maxDistance),Random.nextInt(-maxDistance..maxDistance),Random.nextInt(-maxDistance..maxDistance))


        val balls = mutableListOf<Vector3d>()
        balls.add(center_2.toJOMLD())
        balls.add(center.toJOMLD())

        val noise = PerlinNoise.create(SingleThreadedRandomSource(level.seed), mutableListOf(1,1))
        val resolution = 0.25

        for (x in min(center.x,center_2.x)-(2/threshold).toInt()..max(center.x,center_2.x) +(2/threshold).toInt()) {
            for (y in min(center.y,center_2.y)-(2/threshold).toInt()..max(center.y,center_2.y)+(2/threshold).toInt()) {
                for (z in min(center.z,center_2.z)-(2/threshold).toInt()..max(center.z,center_2.z)+(2/threshold).toInt()) {
                    val t = threshold - noise.getValue(x.toDouble()*resolution,y.toDouble()*resolution,z.toDouble()*resolution)/100
                    val mb = metaBall(balls as List<Vector3d>, BlockPos(x,y,z).toJOMLD())
                    if (mb > 2*t)  {
                        level.setBlockAndUpdate(BlockPos(x,y,z), ClockworkBlocks.WANDERLITE_NYX_ORE.defaultState)
                    } else if (mb > t) {
                        level.setBlockAndUpdate(BlockPos(x,y,z), Blocks.STONE.defaultBlockState())
                    }

                }
            }
        }





    }

    fun metaBall(balls: List<Vector3d>, pos: Vector3d): Double {
        var sigma = 0.0
        for (ball in balls) {
            sigma += 1/ball.distance(pos)
        }
        return sigma
    }


}