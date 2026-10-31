package org.valkyrienskies.clockwork.content.contraptions.phys.infuser
import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.animation.KeyframeAnimations

// Save this class in your mod and generate all required imports
/**
 * Made with Blockbench 5.1.6
 * Exported for Minecraft version 1.19 or later with Mojang mappings
 * @author Author
 */
// Save this class in your mod and generate all required imports
/**
 * Made with Blockbench 5.1.6
 * Exported for Minecraft version 1.19 or later with Mojang mappings
 * @author Author
 */
object PhysicsInfuserAnimations {
    val assemble: AnimationDefinition? = AnimationDefinition.Builder.withLength(10.0f)
        .addAnimation(
            "corners", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "frame", AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                Keyframe(
                    0.0f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.05f,
                    KeyframeAnimations.degreeVec(-0.12f, -45.0f, -0.02f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.1f,
                    KeyframeAnimations.degreeVec(0.09f, -45.0f, -0.23f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.15f,
                    KeyframeAnimations.degreeVec(0.32f, -45.0f, 0.19f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.2f,
                    KeyframeAnimations.degreeVec(-0.32f, -45.0f, 0.38f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.25f,
                    KeyframeAnimations.degreeVec(-0.4f, -45.0f, -0.48f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.3f,
                    KeyframeAnimations.degreeVec(0.65f, -45.0f, -0.37f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.35f,
                    KeyframeAnimations.degreeVec(0.3f, -45.0f, 0.82f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.4f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.45f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.5f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.55f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.6f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.65f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.7f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.75f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.8f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.85f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.9f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    0.95f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.0f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.05f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.1f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.15f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.2f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.25f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.3f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.35f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.4f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.45f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.5f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.55f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.6f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.65f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.7f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.75f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.8f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.85f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.9f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    1.95f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.0f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.05f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.1f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.15f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.2f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.25f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.3f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.35f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.4f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.45f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.5f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.55f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.6f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.65f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.7f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.75f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.8f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.85f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.9f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    2.95f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.0f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.05f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.1f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.15f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.2f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.25f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.3f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.35f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.4f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.45f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.5f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.55f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.6f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.65f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.7f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.75f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.8f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.85f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.9f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    3.95f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.0f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.05f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.1f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.15f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.2f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.25f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.3f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.35f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.4f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.45f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.5f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.55f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.6f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.65f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.7f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.75f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.8f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.85f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.9f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    4.95f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.0f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.05f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.1f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.15f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.2f,
                    KeyframeAnimations.degreeVec(0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.25f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.3f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.35f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.4f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.45f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, -0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.5f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, -0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.55f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, 0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.6f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.65f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.77f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.7f,
                    KeyframeAnimations.degreeVec(0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.75f,
                    KeyframeAnimations.degreeVec(0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.8f,
                    KeyframeAnimations.degreeVec(-0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.85f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, -1.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.9f,
                    KeyframeAnimations.degreeVec(0.98f, -45.0f, 0.17f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    5.95f,
                    KeyframeAnimations.degreeVec(-0.34f, -45.0f, 0.94f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.0f,
                    KeyframeAnimations.degreeVec(-0.87f, -45.0f, -0.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.05f,
                    KeyframeAnimations.degreeVec(0.6f, -45.0f, -0.79f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.1f,
                    KeyframeAnimations.degreeVec(0.7f, -45.0f, 0.78f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.15f,
                    KeyframeAnimations.degreeVec(-0.96f, -45.0f, 0.46f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.2f,
                    KeyframeAnimations.degreeVec(-0.12f, -45.0f, -0.95f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.25f,
                    KeyframeAnimations.degreeVec(0.72f, -45.0f, 0.08f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.3f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.51f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.35f,
                    KeyframeAnimations.degreeVec(-0.61f, -45.0f, 0.19f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.4f,
                    KeyframeAnimations.degreeVec(-0.11f, -45.0f, -0.95f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.45f,
                    KeyframeAnimations.degreeVec(1.14f, -45.0f, 0.38f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.5f,
                    KeyframeAnimations.degreeVec(-0.94f, -45.0f, 0.83f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.55f,
                    KeyframeAnimations.degreeVec(-0.15f, -45.0f, -1.05f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.6f,
                    KeyframeAnimations.degreeVec(0.59f, -45.0f, 0.31f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.65f,
                    KeyframeAnimations.degreeVec(-0.13f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.7f,
                    KeyframeAnimations.degreeVec(0.04f, -45.0f, 0.49f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.75f,
                    KeyframeAnimations.degreeVec(-0.81f, -45.0f, -0.59f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.8f,
                    KeyframeAnimations.degreeVec(1.31f, -45.0f, -0.33f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.85f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, 1.3f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.9f,
                    KeyframeAnimations.degreeVec(-0.44f, -45.0f, -1.23f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    6.95f,
                    KeyframeAnimations.degreeVec(0.83f, -45.0f, 0.52f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.0f,
                    KeyframeAnimations.degreeVec(-0.64f, -45.0f, -0.19f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.05f,
                    KeyframeAnimations.degreeVec(0.76f, -45.0f, 0.22f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.1f,
                    KeyframeAnimations.degreeVec(-1.17f, -45.0f, 0.21f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.15f,
                    KeyframeAnimations.degreeVec(0.99f, -45.0f, -1.14f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.2f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.65f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.25f,
                    KeyframeAnimations.degreeVec(-1.0f, -45.0f, -1.23f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.3f,
                    KeyframeAnimations.degreeVec(1.32f, -45.0f, 0.42f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.35f,
                    KeyframeAnimations.degreeVec(-1.23f, -45.0f, 0.14f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.4f,
                    KeyframeAnimations.degreeVec(1.15f, -45.0f, -0.57f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.45f,
                    KeyframeAnimations.degreeVec(-0.92f, -45.0f, 1.19f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.5f,
                    KeyframeAnimations.degreeVec(0.16f, -45.0f, -1.72f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.55f,
                    KeyframeAnimations.degreeVec(0.89f, -45.0f, 1.6f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.6f,
                    KeyframeAnimations.degreeVec(-1.63f, -45.0f, -0.84f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.65f,
                    KeyframeAnimations.degreeVec(1.78f, -45.0f, -0.11f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.7f,
                    KeyframeAnimations.degreeVec(-1.51f, -45.0f, 0.93f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.75f,
                    KeyframeAnimations.degreeVec(0.93f, -45.0f, -1.59f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.8f,
                    KeyframeAnimations.degreeVec(-0.02f, -45.0f, 1.93f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.85f,
                    KeyframeAnimations.degreeVec(-1.0f, -45.0f, -1.73f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.9f,
                    KeyframeAnimations.degreeVec(2.47f, -45.0f, 2.42f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    7.95f,
                    KeyframeAnimations.degreeVec(-4.33f, -45.0f, 2.5f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.0f,
                    KeyframeAnimations.degreeVec(-1.6f, -45.0f, -4.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.05f,
                    KeyframeAnimations.degreeVec(2.83f, -45.0f, -0.6f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.1f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 1.18f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.15f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.2f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.25f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.3f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.35f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.4f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.45f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.5f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.55f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.6f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.65f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.7f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.75f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.8f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.85f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.9f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.95f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.0f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.05f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.1f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.15f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.2f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.25f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.3f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.35f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.4f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.45f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.5f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.55f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.6f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.65f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.7f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.75f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.8f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.85f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.9f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    9.95f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    10.0f,
                    KeyframeAnimations.degreeVec(0.0f, -45.0f, 0.0f),
                    AnimationChannel.Interpolations.LINEAR
                )
            )
        )
        .addAnimation(
            "frame", AnimationChannel(
                AnimationChannel.Targets.POSITION,
                Keyframe(0.4f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.45f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.5f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.55f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.6f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.65f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.7f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.75f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.8f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.85f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.9f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(0.95f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.0f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.05f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.1f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.15f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.2f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.25f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.3f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.35f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.4f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.45f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.5f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.55f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.6f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.65f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.7f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.75f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.8f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.85f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.9f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(1.95f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.0f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.05f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.1f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.15f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.2f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.25f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.3f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.35f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.4f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.45f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.5f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.55f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.6f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.65f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.7f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.75f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.8f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.85f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.9f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(2.95f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.0f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.05f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.1f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.15f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.2f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.25f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.3f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.35f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.4f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.45f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.5f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.55f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.6f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.65f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.7f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.75f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.8f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.85f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.9f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(3.95f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.0f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.05f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.1f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.15f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.2f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.25f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.3f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.35f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.4f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.45f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.5f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.55f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.6f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.65f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.7f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.75f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.8f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.85f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.9f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(4.95f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.0f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.05f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.1f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.15f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.2f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.25f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.3f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.35f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.4f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.45f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.5f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.55f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.6f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.65f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.7f, KeyframeAnimations.posVec(0.0f, 0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.75f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.8f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.85f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.9f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(5.95f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.0f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.05f, KeyframeAnimations.posVec(0.0f, 0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.1f, KeyframeAnimations.posVec(0.0f, -0.06f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.15f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.2f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.25f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.3f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.35f, KeyframeAnimations.posVec(0.0f, -0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.4f, KeyframeAnimations.posVec(0.0f, 0.03f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.45f, KeyframeAnimations.posVec(0.0f, -0.01f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.5f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.55f, KeyframeAnimations.posVec(0.0f, -0.08f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.6f, KeyframeAnimations.posVec(0.0f, 0.04f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.65f, KeyframeAnimations.posVec(0.0f, 0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.7f, KeyframeAnimations.posVec(0.0f, 0.01f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.75f, KeyframeAnimations.posVec(0.0f, -0.05f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.8f, KeyframeAnimations.posVec(0.0f, -0.01f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.85f, KeyframeAnimations.posVec(0.0f, 0.08f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.9f, KeyframeAnimations.posVec(0.0f, -0.03f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(6.95f, KeyframeAnimations.posVec(0.0f, -0.03f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.0f, KeyframeAnimations.posVec(0.0f, -0.03f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.05f, KeyframeAnimations.posVec(0.0f, 0.09f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.1f, KeyframeAnimations.posVec(0.0f, -0.02f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.15f, KeyframeAnimations.posVec(0.0f, -0.07f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.2f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.25f, KeyframeAnimations.posVec(0.0f, 0.08f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.3f, KeyframeAnimations.posVec(0.0f, 0.01f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.35f, KeyframeAnimations.posVec(0.0f, -0.11f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.4f, KeyframeAnimations.posVec(0.0f, 0.01f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.45f, KeyframeAnimations.posVec(0.0f, 0.08f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.5f, KeyframeAnimations.posVec(0.0f, 0.01f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.55f, KeyframeAnimations.posVec(0.0f, -0.11f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.6f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.65f, KeyframeAnimations.posVec(0.0f, 0.11f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.7f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.75f, KeyframeAnimations.posVec(0.0f, -0.12f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.8f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.85f, KeyframeAnimations.posVec(0.0f, 0.13f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.9f, KeyframeAnimations.posVec(0.0f, 0.1f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(7.95f, KeyframeAnimations.posVec(0.0f, 0.1f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(8.0f, KeyframeAnimations.posVec(0.0f, 0.1f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                Keyframe(8.05f, KeyframeAnimations.posVec(0.0f, 0.1f, 0.0f), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "vortex", AnimationChannel(
                AnimationChannel.Targets.SCALE,
                Keyframe(7.95f, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR),
                Keyframe(
                    8.0f,
                    KeyframeAnimations.scaleVec(0.1309, 0.1309, 0.1309),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.05f,
                    KeyframeAnimations.scaleVec(0.3746, 0.3746, 0.3746),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.1f,
                    KeyframeAnimations.scaleVec(0.5341, 0.5341, 0.5341),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.15f,
                    KeyframeAnimations.scaleVec(0.645, 0.645, 0.645),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.2f,
                    KeyframeAnimations.scaleVec(0.7276, 0.7276, 0.7276),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.25f,
                    KeyframeAnimations.scaleVec(0.7917, 0.7917, 0.7917),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.3f,
                    KeyframeAnimations.scaleVec(0.8423, 0.8423, 0.8423),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.35f,
                    KeyframeAnimations.scaleVec(0.8827, 0.8827, 0.8827),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.4f,
                    KeyframeAnimations.scaleVec(0.915, 0.915, 0.915),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.45f,
                    KeyframeAnimations.scaleVec(0.9406, 0.9406, 0.9406),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.5f,
                    KeyframeAnimations.scaleVec(0.9606, 0.9606, 0.9606),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.55f,
                    KeyframeAnimations.scaleVec(0.9758, 0.9758, 0.9758),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.6f,
                    KeyframeAnimations.scaleVec(0.9869, 0.9869, 0.9869),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.65f,
                    KeyframeAnimations.scaleVec(0.9944, 0.9944, 0.9944),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(
                    8.7f,
                    KeyframeAnimations.scaleVec(0.9986, 0.9986, 0.9986),
                    AnimationChannel.Interpolations.LINEAR
                ),
                Keyframe(8.75f, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build()
}