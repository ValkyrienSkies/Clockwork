package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.AllBlocks
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import org.valkyrienskies.clockwork.ponderLang

object OtherPonders {

    fun solid_delivery(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)
        scene.title("solid_delivery", "Solid delivery")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)
        val depotLine = util.select().fromTo(0, 1, 2, 4, 2, 2)
        val chuteLine = util.select().fromTo(0, 1, 3, 4, 2, 3)
        val beltLine = util.select().fromTo(0, 1, 4, 4, 2, 4)
        val depotCannonPos = BlockPos(0, 2, 2)
        val depotChutePos = BlockPos(4, 2, 2)

        val cannonSlot = util.vector().blockSurface(depotCannonPos, Direction.NORTH)
            .add(0.0, -0.35, 0.0)

        val chuteSlot = util.vector().blockSurface(depotChutePos, Direction.NORTH)
            .add(0.0, -0.35, 0.0)

        scene.world().showSection(depotLine, Direction.DOWN)
        scene.overlay().showText(80)
            .text(scene.ponderLang(1))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(depotCannonPos, Direction.WEST))

        scene.idle(80+20)

        scene.overlay().showFilterSlotInput(cannonSlot, Direction.NORTH, 60)
        scene.overlay().showFilterSlotInput(chuteSlot, Direction.NORTH, 60)

        scene.overlay().showText(80)
            .attachKeyFrame()
            .placeNearTarget()
            .text(scene.ponderLang(2))
            .pointAt(cannonSlot)

        scene.idle(80+20)

        scene.overlay().showText(60)
            .placeNearTarget()
            .text(scene.ponderLang(3))
            .pointAt(chuteSlot)

        scene.idle(60+20)

        scene.overlay().showText(80)
            .attachKeyFrame()
            .text(scene.ponderLang(4))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(depotCannonPos.below(), Direction.WEST))

        scene.idle(80+20)

        // region Temporarily hide the delivery cannon/chute
        scene.world().hideSection(depotLine, Direction.UP)
        scene.idle(15)
        scene.world().setBlock(depotCannonPos, Blocks.AIR.defaultBlockState(), false)
        scene.world().setBlock(depotChutePos, Blocks.AIR.defaultBlockState(), false)
        // endregion

        // region Show the section but with weighted ejector
        scene.world().setBlock(depotCannonPos.below(), AllBlocks.WEIGHTED_EJECTOR.defaultState.rotate(Rotation.CLOCKWISE_90), false)
        scene.world().showSection(depotLine, Direction.DOWN)
        scene.idle(15)
        // endregion

        scene.overlay().showText(80)
            .attachKeyFrame()
            .text(scene.ponderLang(5))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(depotCannonPos.below(), Direction.WEST))

        scene.idle(80+20)

        // region Restore to original cannon+chute
        scene.world().hideSection(depotLine, Direction.UP)
        scene.idle(15)
        scene.world().restoreBlocks(depotLine)
        scene.idle(5)
        scene.world().showSection(depotLine, Direction.DOWN)
        scene.idle(15)
        // endregion

        scene.overlay().showText(80)
            .text(scene.ponderLang(6))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(depotCannonPos, Direction.WEST))

        scene.idle(80+20)

        // region "can be placed on depot, chute, belts" section
        scene.addKeyframe()
        scene.idle(20)

        scene.overlay().showText(40)
            .text(scene.ponderLang(7))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(depotChutePos.below(), Direction.WEST))
        scene.idle(40+20)

        scene.world().hideSection(depotLine, Direction.DOWN)
        scene.idle(10)
        scene.world().showSection(chuteLine, Direction.DOWN)

        scene.overlay().showText(40)
            .text(scene.ponderLang(8))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(4, 1, 3), Direction.WEST))
        scene.idle(40+20)

        scene.world().hideSection(chuteLine, Direction.DOWN)
        scene.idle(10)
        scene.world().showSection(beltLine, Direction.DOWN)

        scene.overlay().showText(40)
            .text(scene.ponderLang(9))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(4, 1, 4), Direction.WEST))
        scene.idle(40+20)
        // endregion

        scene.markAsFinished()
    }

}
