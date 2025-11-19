package org.valkyrienskies.clockwork.util.render

import net.createmod.catnip.theme.Color
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4ic
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.random.Random

object BoltUtil {

    private val rand: Random = Random.Default
    private val bolts: ArrayList<Bolt> = ArrayList()

    fun addBolt(start: Vector3dc, end: Vector3dc, color: Vector4ic, width: Float, segments: Int? = null): Bolt {
        val seg: Int = segments ?: (start.distance(end) / 0.5).toInt()

        val segList: ArrayList<Vector3dc> = ArrayList()
        segList.add(start)
        for (i in 0..seg) {
            val segment: Vector3d = start.lerp(end, i.toDouble(), Vector3d())
            segList.add(segment)
        }
        segList.add(end)
        val bolt: Bolt = Bolt(segList)
        bolts.add(bolt)

        for (b in 0 until bolt.segments.size - 1) {
            val segment: Vector3dc = bolt.segments[b]
            val nextSegment: Vector3dc = bolt.segments[b + 1]
            ClockworkModClient.OUTLINER.showLine(segment, segment.toMinecraft(), nextSegment.toMinecraft())
            ClockworkModClient.OUTLINER.edit(segment).ifPresent {
                it.colored(Color(color.x(), color.y(), color.z(), color.w()))
                it.lineWidth(width)
            }
        }

        return bolt
    }

    fun delBolt(bolt: Bolt) {
        bolts.remove(bolt)
        for (segment in bolt.segments) {
            ClockworkModClient.OUTLINER.remove(segment)
        }
    }

    fun retargetBolt(bolt: Bolt, start: Vector3dc? = null, end: Vector3dc? = null) {
        if (bolts.contains(bolt)) {
            bolts.remove(bolt)
            if (start != null) {
                bolt.segments[0] = start
            }
            if (end != null) {
                bolt.segments[bolt.segments.size - 1] = end
            }
            bolts.add(bolt)
        }
    }

    fun tick() {
        if (bolts.isEmpty()) return
        val boltsCopied = ArrayList(bolts)
        for (bolt in boltsCopied) {
            var prevSegmentEnd: Vector3dc = bolt.segments[1]
            ClockworkModClient.OUTLINER.showLine(
                bolt.segments[0],
                bolt.segments[0].toMinecraft(),
                prevSegmentEnd.toMinecraft()
            )
            for (i in 1 until bolt.segments.size - 1) {
                val segment: Vector3d = bolt.segments[i] as Vector3d
                val nextSegment: Vector3d = bolt.segments[i + 1] as Vector3d
                val xOffset: Double = rand.nextDouble(-0.5, 0.5)
                val yOffset: Double = rand.nextDouble(-0.5, 0.5)
                val zOffset: Double = rand.nextDouble(-0.5, 0.5)
                nextSegment.add(xOffset, yOffset, zOffset)
                ClockworkModClient.OUTLINER.showLine(segment, prevSegmentEnd.toMinecraft(), nextSegment.toMinecraft())
                prevSegmentEnd = nextSegment
            }
            ClockworkModClient.OUTLINER.showLine(
                prevSegmentEnd,
                prevSegmentEnd.toMinecraft(),
                bolt.segments[bolt.segments.size - 1].toMinecraft()
            )
        }
    }
}
