package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.simibubi.create.content.contraptions.StructureTransform
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import kotlin.math.roundToInt

/** The 24 lattice rotations, expressed as Create transforms for blocks and block entities too. */
class WandGridRotation(private val axes: List<Direction.Axis>) {
    val steps: List<StructureTransform> get() = axes.map {
        StructureTransform(BlockPos.ZERO, it, Rotation.CLOCKWISE_90, Mirror.NONE)
    }
    // Exact signed permutations avoid floating point floor errors at negative block coordinates.
    fun vector(v: Vec3): Vec3 = axes.fold(v) { p, axis ->
        val x = Vec3.atLowerCornerOf(Direction.EAST.getClockWise(axis).normal)
        val y = Vec3.atLowerCornerOf(Direction.UP.getClockWise(axis).normal)
        val z = Vec3.atLowerCornerOf(Direction.SOUTH.getClockWise(axis).normal)
        x.scale(p.x).add(y.scale(p.y)).add(z.scale(p.z))
    }
    fun offset(pos: BlockPos): BlockPos {
        val p = vector(Vec3.atLowerCornerOf(pos))
        return BlockPos(p.x.roundToInt(), p.y.roundToInt(), p.z.roundToInt())
    }
    fun face(face: Direction): Direction {
        val v = vector(Vec3.atLowerCornerOf(face.normal))
        return Direction.getNearest(v.x, v.y, v.z)
    }

    companion object {
        val all: List<WandGridRotation> by lazy {
            val found = linkedMapOf<List<BlockPos>, WandGridRotation>()
            val queue = ArrayDeque<WandGridRotation>()
            queue.add(WandGridRotation(emptyList()))
            while (queue.isNotEmpty()) {
                val r = queue.removeFirst()
                val key = listOf(BlockPos(1, 0, 0), BlockPos(0, 1, 0), BlockPos(0, 0, 1)).map(r::offset)
                if (found.putIfAbsent(key, r) != null) continue
                for (axis in Direction.Axis.values()) queue.add(WandGridRotation(r.axes + axis))
            }
            found.values.toList()
        }

        fun choose(from: Direction, to: Direction, sourceRotation: Quaterniondc = Quaterniond(),
                   targetRotation: Quaterniondc = Quaterniond()): WandGridRotation {
            val relative = Quaterniond(targetRotation).invert().mul(sourceRotation)
            val axes = listOf(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Vec3(0.0, 0.0, 1.0))
            return all.filter { it.face(from) == to.opposite }.maxBy { r ->
                axes.sumOf { axis ->
                    val desired = relative.transform(Vector3d(axis.x, axis.y, axis.z))
                    val actual = r.vector(axis)
                    actual.x * desired.x + actual.y * desired.y + actual.z * desired.z
                }
            }
        }
    }
}
