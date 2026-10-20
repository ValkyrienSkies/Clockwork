package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.animation.KeyframeAnimations

object PhysicsInfuserAnimationTest {
    val assembler: AnimationDefinition? = AnimationDefinition.Builder.withLength(25.0f)
        .addAnimation(
            "frame", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    12.0f,
                    KeyframeAnimations.degreeVec(0.0f, -225.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    25.0f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "zap", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                Keyframe(5.0f, KeyframeAnimations.posVec(0.0f, 15.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(22.0f, KeyframeAnimations.posVec(0.0f, 15.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(23.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)
            )
        )
        .addAnimation(
            "vortex", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "vortex", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(23.0f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(24.0f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.CATMULLROM)
            )
        )
        .build()
}