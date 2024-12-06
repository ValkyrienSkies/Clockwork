package org.valkyrienskies.clockwork.content.curiosities.asteroid

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.loader.impl.lib.sat4j.core.Vec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.synth.PerlinNoise
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt


object meteoroidGenerator {

    fun generate(level: ServerLevel,center: BlockPos, threshold: Double, maxDistance: Int) {

        val center_2 = center.offset(Random.nextInt(1..maxDistance),Random.nextInt(1..maxDistance),Random.nextInt(1..maxDistance))


        val balls = mutableListOf<Vector3d>()
        balls.add(center_2.toJOMLD())
        balls.add(center.toJOMLD())


        for (x in min(center.x,center_2.x)-(2/threshold).toInt()..max(center.x,center_2.x) +(2/threshold).toInt()) {
            for (y in min(center.y,center_2.y)-(2/threshold).toInt()..max(center.y,center_2.y)+(2/threshold).toInt()) {
                for (z in min(center.z,center_2.z)-(2/threshold).toInt()..max(center.z,center_2.z)+(2/threshold).toInt()) {
                    if (metaBall(threshold, balls as List<Vector3d>, BlockPos(x,y,z).toJOMLD()))  {
                        level.setBlockAndUpdate(BlockPos(x,y,z), Blocks.STONE.defaultBlockState())
                    }

                }
            }
        }



    }

    fun metaBall(threshold: Double, balls: List<Vector3d>, pos: Vector3d): Boolean {
        var sigma = 0.0
        for (ball in balls) {
            sigma += 1/ball.distance(pos)
        }
        println(sigma)
        return sigma > threshold
    }
}