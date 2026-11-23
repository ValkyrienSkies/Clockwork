package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.AllItems
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctConnectionType
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctEdgeType
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity

object KelvinPonders {
    fun duct(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)
        scene.title("duct", "Managing Gas Networks")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val duct1 = util.grid().at(2, 1, 2)
        val duct2 = util.grid().at(2, 1, 3)
        val duct3 = util.grid().at(2, 1, 4)
        val ductGroup = util.select().fromTo(1, 1, 2, 3, 1, 4)
        val edgeVec = util.vector().centerOf(duct1).add(0.0, 0.0, 0.5)

        fun setEdgeType(type: DuctEdgeType) {
            scene.addInstruction { step ->
                val first = step.world.getBlockEntity(duct1) as? DuctBlockEntity ?: return@addInstruction
                val second = step.world.getBlockEntity(duct2) as? DuctBlockEntity ?: return@addInstruction
                first.setEdgeType(Direction.SOUTH, second.getDuctNodePosition(), type, clientPacket = false)
                second.DIR_TO_CONNECTION_TYPE[Direction.NORTH] = type
            }
        }

        scene.world().modifyBlockEntity(util.grid().at(3, 1, 2), CreativeGeneratorBlockEntity::class.java) { generator ->
            generator.gasValues[ClockworkGasses.STEAM] = 250
            generator.temperature = 373.15
        }

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
            .text("Gas blocks can only be connected to each other with ducts.")
            .pointAt(util.vector().topOf(duct2))
            .placeNearTarget()

        scene.idle(90)

        // Wrench mechanics
        scene.overlay().showControls(
            edgeVec, Pointing.DOWN, 6
        ).rightClick().withItem(AllItems.WRENCH.asStack())

        scene.idle(10)

        scene.overlay().showText(70)
            .text("Using a Wrench on a connection toggles it on or off.")
            .attachKeyFrame()
            .pointAt(edgeVec)
            .placeNearTarget()

        // Visual toggle to forced off
        scene.world().modifyBlock(duct1, { s: BlockState -> s.setValue(DuctBlock.DIR_TO_CONNECTION[Direction.SOUTH]!!, DuctConnectionType.FORCED_OFF) }, true)
        scene.world().modifyBlock(duct2, { s: BlockState -> s.setValue(DuctBlock.DIR_TO_CONNECTION[Direction.NORTH]!!, DuctConnectionType.FORCED_OFF) }, true)
        //scene.world().modifyBlock(duct3, { s: BlockState -> s.setValue(DuctBlock.NORTH_CONNECTION, DuctConnectionType.FORCED_OFF) }, true)
        scene.idle(40)

        // Visual toggle back to side
        scene.world().modifyBlock(duct1, { s: BlockState -> s.setValue(DuctBlock.DIR_TO_CONNECTION[Direction.SOUTH]!!, DuctConnectionType.SIDE) }, true)
        scene.world().modifyBlock(duct2, { s: BlockState -> s.setValue(DuctBlock.DIR_TO_CONNECTION[Direction.NORTH]!!, DuctConnectionType.SIDE) }, true)
        //scene.world().modifyBlock(duct3, { s: BlockState -> s.setValue(DuctBlock.NORTH_CONNECTION, DuctConnectionType.SIDE) }, true)
        scene.idle(40)

        // Screwdriver configuration
        scene.overlay().showControls(
            edgeVec, Pointing.DOWN, 15
        ).whileSneaking().rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())

        scene.idle(10)

        scene.overlay().showText( 50)
            .text("Shift right clicking an edge with a Screwdriver cycles its type. The types are:")
            .attachKeyFrame()
            .pointAt(edgeVec)

        scene.idle(55)

        // One-Way
        scene.overlay().showText( 30)
            .text("One-way")
            .attachKeyFrame()
            .pointAt(edgeVec)

        setEdgeType(DuctEdgeType.ONEWAY_BACKWARD)
        scene.idle(40)

        scene.overlay().showText( 30)
            .text("Filtered")
            .attachKeyFrame()
            .pointAt(edgeVec)

        // Filtered
        setEdgeType(DuctEdgeType.FILTERED)
        scene.idle(40)

        // Smart

        scene.overlay().showText( 50)
            .text("Smart")
            .attachKeyFrame()
            .pointAt(edgeVec)
        setEdgeType(DuctEdgeType.SMART)
        scene.idle(60)

        // Screwdriving

        scene.overlay().showText( 50)
            .text("Right clicking a Smart or Filtered edge with a Screwdriver lets you edit it")
            .attachKeyFrame()
            .pointAt(edgeVec)

        scene.overlay().showControls(
            edgeVec, Pointing.DOWN, 20
        ).rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())

        scene.idle(80)

        // Leaks
        // Visually create a leak!
        scene.world().modifyBlock(duct3, { s: BlockState -> s.setValue(DuctBlock.DIR_TO_CONNECTION[Direction.UP]!!, DuctConnectionType.LEAK) }, true)

        scene.overlay().showText(70)
            .text("If a duct is caught in an explosion, it will become a leak and gas will violently escape!")
            .attachKeyFrame()
//            .pointAt(util.vector().topOf(duct3))
//            .placeNearTarget()
        scene.idle(80)

        scene.overlay().showControls(
            util.vector().topOf(duct3), Pointing.DOWN, 60
        ).rightClick().withItem(AllItems.WRENCH.asStack())

        scene.idle(10)

        scene.overlay().showText(60)
            .text("Right-Clicking a leak with a Wrench will seal it back up.")
            .pointAt(util.vector().topOf(duct3))
            .placeNearTarget()

        scene.idle(40)

        // Seal the leak
        scene.world().modifyBlock(duct3, { s: BlockState -> s.setValue(DuctBlock.DIR_TO_CONNECTION[Direction.UP]!!, DuctConnectionType.NONE) }, true)

        scene.idle(70)
    }
}
