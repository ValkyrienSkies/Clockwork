package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.animation.KeyframeAnimations

object PhysBearingAnimations {
    val opening: AnimationDefinition? = AnimationDefinition.Builder.withLength(1.35f)
        .addAnimation(
            "attacher", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.7083f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.0417f, KeyframeAnimations.posVec(0.0f, 1.2f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.3333f, KeyframeAnimations.posVec(0.0f, 3.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "westWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.7083f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.125f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 135.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.3333f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 135.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "eastWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.7083f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.125f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, -135.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.3333f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, -135.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "northWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.7083f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.9167f,
                    KeyframeAnimations.degreeVec(6.67f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    1.125f,
                    KeyframeAnimations.degreeVec(27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    1.3333f,
                    KeyframeAnimations.degreeVec(27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "northWing", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.125f, KeyframeAnimations.posVec(0.0f, 0.75f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "southWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.7083f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.9167f,
                    KeyframeAnimations.degreeVec(-6.67f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    1.125f,
                    KeyframeAnimations.degreeVec(-27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    1.3333f,
                    KeyframeAnimations.degreeVec(-27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "southWing", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.125f, KeyframeAnimations.posVec(0.0f, 0.75f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.3333f, KeyframeAnimations.posVec(0.0f, 0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.4167f, KeyframeAnimations.posVec(0.0f, 0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()

    val closing: AnimationDefinition? = AnimationDefinition.Builder.withLength(1.35f)
        .addAnimation(
            "attacher", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 3.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.2917f, KeyframeAnimations.posVec(0.0f, 1.2f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.625f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "westWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 135.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.2083f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 135.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.625f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "eastWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, -135.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.2083f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, -135.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(0.625f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "northWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.2083f,
                    KeyframeAnimations.degreeVec(27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.4167f,
                    KeyframeAnimations.degreeVec(6.67f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.625f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "northWing", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(1.0f, KeyframeAnimations.posVec(0.0f, 0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.2083f, KeyframeAnimations.posVec(0.0f, 0.75f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "southWing", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(-27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.2083f,
                    KeyframeAnimations.degreeVec(-27.5f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.4167f,
                    KeyframeAnimations.degreeVec(-6.67f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                ),
                Keyframe(
                    0.625f,
                    KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f),
                    AnimationChannel.Interpolations.CATMULLROM
                )
            )
        )
        .addAnimation(
            "southWing", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.9167f, KeyframeAnimations.posVec(0.0f, 0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.0f, KeyframeAnimations.posVec(0.0f, 0.5f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.2083f, KeyframeAnimations.posVec(0.0f, 0.75f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.3333f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()
}