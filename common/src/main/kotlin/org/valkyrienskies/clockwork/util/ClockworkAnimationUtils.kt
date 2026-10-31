package org.valkyrienskies.clockwork.util

import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.util.render.TransformData
import kotlin.collections.firstOrNull

class ClockworkAnimationUtils {
    fun AnimationDefinition.findChannel(
        bone: String,
        target: AnimationChannel.Target
    ): AnimationChannel? {
        return this.boneAnimations[bone]
            ?.firstOrNull { it.target == target }
    }

    fun applyAnimationToPartial(
        partial: SuperByteBuffer,
        anim: AnimationDefinition,
        bone: String,
        pivot: Vec3,
        time: Float
    ) {
        val piv = Vec3(pivot.x+0.5, pivot.y, pivot.z+0.5)
        val orb_offset: Vector3f
        anim.findChannel(bone, AnimationChannel.Targets.ROTATION)
            ?.keyframes
            ?.toList()
            ?.let { frames ->
                val rot = interpolateKeyframes(frames, time)
                partial.translate(piv)
                partial.rotateDegrees((180f / Math.PI.toFloat()) * rot.x, Direction.Axis.X)
                partial.rotateDegrees((180f / Math.PI.toFloat()) * rot.y, Direction.Axis.Y)
                partial.rotateDegrees((180f / Math.PI.toFloat()) * rot.z, Direction.Axis.Z)
                partial.translateBack(piv)
            }

        anim.findChannel(bone, AnimationChannel.Targets.SCALE)
            ?.keyframes
            ?.toList()
            ?.let { frames ->
                val scl = interpolateKeyframes(frames, time)
                partial.translate(piv)
                // animation scale is relative; maintain base 1.0
                partial.scale(scl.add(1f, 1f, 1f))
                partial.translateBack(piv)
            }

        anim.findChannel(bone, AnimationChannel.Targets.POSITION)
            ?.keyframes
            ?.toList()
            ?.let { frames ->
                val pos = interpolateKeyframes(frames, time)
                partial.translate(-pos.x.toDouble() / 16, -pos.y.toDouble() / 16, -pos.z.toDouble() / 16)
            }
    }

    fun applyAnimationToTransform(
        transform: TransformData,
        anim: AnimationDefinition,
        bone: String,
        pivot: Vec3,
        time: Float
    ) {
        val piv = Vector3f(pivot.x.toFloat()+0.5f, pivot.y.toFloat(), pivot.z.toFloat()+0.5f)
        anim.findChannel(bone, AnimationChannel.Targets.ROTATION)
            ?.keyframes
            ?.toList()
            ?.let { frames ->
                val rot = interpolateKeyframes(frames, time)
                transform.offset.add(piv)
                transform.rotation.rotate(Quaternionf().rotateXYZ((180f / Math.PI.toFloat()) * rot.x,(180f / Math.PI.toFloat()) * rot.y,(180f / Math.PI.toFloat()) * rot.z))
                transform.offset.add(piv.negate())
            }

        anim.findChannel(bone, AnimationChannel.Targets.POSITION)
            ?.keyframes
            ?.toList()
            ?.let { frames ->
                val pos = interpolateKeyframes(frames, time)
                transform.offset.add(-pos.x / 16, -pos.y / 16, -pos.z / 16)
            }
    }


    fun interpolateKeyframes(
        keyframes: List<Keyframe>,
        time: Float
    ): Vector3f {
        if (keyframes.isEmpty()) return Vector3f()

        val before = keyframes.lastOrNull { it.timestamp <= time } ?: keyframes.first()
        val after = keyframes.firstOrNull { it.timestamp >= time } ?: keyframes.last()

        if (before === after) return Vector3f(before.target[0], before.target[1], before.target[2])

        val t = (time - before.timestamp) / (after.timestamp - before.timestamp)

        val p1 = before.target
        val p2 = after.target
        val p0 = keyframes.getOrNull(keyframes.indexOf(before) - 1)?.target ?: p1
        val p3 = keyframes.getOrNull(keyframes.indexOf(after) + 1)?.target ?: p2

        if (before.interpolation == AnimationChannel.Interpolations.LINEAR) return Vector3f(
            p1[0] + (p2[0] - p1[0]) * t,
            p1[1] + (p2[1] - p1[1]) * t,
            p1[2] + (p2[2] - p1[2]) * t
        )
        else
            return Vector3f(
                Mth.catmullrom(t,p0[0], p1[0], p2[0], p3[0]),
                Mth.catmullrom(t,p0[1], p1[1], p2[1], p3[1]),
                Mth.catmullrom(t,p0[2], p1[2], p2[2], p3[2])
            )
    }
}