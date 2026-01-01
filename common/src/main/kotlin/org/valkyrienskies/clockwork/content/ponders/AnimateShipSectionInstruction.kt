package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.ponder.api.element.ElementLink
import net.createmod.ponder.api.element.WorldSectionElement
import net.createmod.ponder.foundation.PonderScene
import net.createmod.ponder.foundation.instruction.AnimateElementInstruction
import net.minecraft.world.phys.Vec3

class AnimateShipSectionInstruction(
    link: ElementLink<WorldSectionElement>,
    ticks: Int,
    private val initialVelocity: Vec3,
    private val initialRotVelocity: Vec3
) : AnimateElementInstruction<WorldSectionElement>(
    link,
    Vec3(0.0, 0.0, 0.0), // we don't use
    ticks,
    { _: WorldSectionElement, _: Vec3 -> }, // we don't use
    { _: WorldSectionElement -> Vec3(0.0, 0.0, 0.0) } // we don't use
) {

    override fun tick(scene: PonderScene) {
        super.tick(scene)
        val delta = 1.0 - (remainingTicks.toDouble() / totalTicks.toDouble())

        element.setAnimatedOffset(element.animatedOffset.add(
            initialVelocity.lerp(Vec3(0.0, 0.0, 0.0),  delta)
        ), false)

        element.setAnimatedRotation(element.animatedRotation.add(
            initialRotVelocity.lerp(Vec3(0.0, 0.0, 0.0), delta)
        ), false)

    }
}

/**
 * Will apply the given [initialVel] and [initialRotVel] to the [link] once,
 * and then slowly decrease its velocity until it stops after [duration] ticks.
 *
 * @param initialVel blocks/tick
 * @param initialRotVel euler angles/tick
 */
fun CreateSceneBuilder.WorldInstructions.moveSectionAsShip(scene: CreateSceneBuilder, link: ElementLink<WorldSectionElement>, duration: Int, initialVel: Vec3 = Vec3(0.0, 0.0, 0.0), initialRotVel: Vec3 = Vec3(0.0, 0.0, 0.0)) {
    scene.addInstruction(AnimateShipSectionInstruction(link, duration, initialVel, initialRotVel))
}