package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.AllItems
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.createmod.ponder.foundation.PonderSceneBuilder
import net.createmod.ponder.foundation.element.InputWindowElement
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkItems

object KelvinPonders {
    fun duct(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)
        scene.title("duct", "Managing Gas Networks")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        // Select the ducts in the schematic (Assuming a 3-long line of ducts in the center, from z=2 to z=4)
        val duct1 = util.grid().at(2, 1, 2)
        val duct2 = util.grid().at(2, 1, 3)
        val duct3 = util.grid().at(2, 1, 4)
        val ductGroup = util.select().fromTo(2, 1, 2, 2, 1, 4)

        // Drop the ducts into the scene
        scene.world().showSection(ductGroup, Direction.DOWN)
        scene.idle(20)

        // Intro text
        scene.overlay().showText(60)
            .text("Ducts form the core of Clockwork's gas networks.")
            .attachKeyFrame()
            .pointAt(util.vector().topOf(duct2))
            .placeNearTarget()
        scene.idle(70)

        scene.overlay().showText(80)
            .text("Gas flows automatically between connected ducts, moving from high to low pressure.")
            .pointAt(util.vector().topOf(duct2))
            .placeNearTarget()
        scene.idle(90)

        // Wrench mechanics
        scene.overlay().showControls(
            util.vector().topOf(duct2), Pointing.DOWN, 60
        ).rightClick().withItem(AllItems.WRENCH.asStack())

        scene.idle(10)

        scene.overlay().showText(70)
            .text("Using a Wrench on a connection toggles it on or off.")
            .attachKeyFrame()
            .pointAt(util.vector().centerOf(duct2))
            .placeNearTarget()
        scene.idle(80)

        // Screwdriver configuration
        scene.overlay().showControls(
            util.vector().topOf(duct2), Pointing.DOWN, 60
        ).rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())

        scene.idle(10)

        scene.overlay().showControls(
            util.vector().topOf(duct2), Pointing.DOWN, 60
        ).rightClick().whileSneaking().withItem(ClockworkItems.SCREWDRIVER.asStack())

        scene.overlay().showOutlineWithText(util.select().position(duct2), 80)
            .text("Sneak right-clicking with a Screwdriver cycles edge types: Pipe, One-Way, Filtered, and Smart.")
            .attachKeyFrame()
            .pointAt(util.vector().centerOf(duct2))
            .placeNearTarget()
        scene.idle(90)

        // Sneak-screwdriving


        scene.idle(10)

        scene.overlay().showControls(
            util.vector().topOf(duct2), Pointing.DOWN, 60
        ).rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())
        scene.overlay().showText(80)
            .text("Right clicking an edge with a screwdriver allows you to edit it (For Filtered and Smart edges).")
            .pointAt(util.vector().centerOf(duct2))
            .placeNearTarget()
        scene.idle(90)

        // Leaks
        scene.overlay().showText(70)
            .text("If a duct is caught in an explosion, it will become a leak and gas will violently escape!")
            .attachKeyFrame()
            .pointAt(util.vector().topOf(duct3))
            .placeNearTarget()
        scene.idle(80)

        scene.overlay().showControls(
            util.vector().topOf(duct3), Pointing.DOWN, 60
        ).rightClick().withItem(AllItems.WRENCH.asStack())

        scene.idle(10)

        scene.overlay().showText(70)
            .text("Right-Clicking a leak with a Wrench will seal it back up.")
            .pointAt(util.vector().topOf(duct3))
            .placeNearTarget()
        scene.idle(80)
    }
}
