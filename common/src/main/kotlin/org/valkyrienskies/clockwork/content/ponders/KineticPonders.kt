package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.state.BlockState

object KineticPonders {
    fun redstoneResistor(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)
        scene.title("resistor", "Using the Redstone Resisitor")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val lever = util.select().position(2, 1, 1)
        val resistor = util.select().position(2, 1, 3)
        val redstone = util.select().position(2, 1, 2)
        val rot1 = util.select().fromTo(0, 1, 3, 1, 1, 3)
        val rot2 = util.select().fromTo(3, 1, 3, 4, 1, 3)

        scene.world().showSection(rot2, Direction.DOWN)
        scene.idle(5)
        scene.world().showSection(resistor, Direction.DOWN)
        scene.idle(5)
        scene.world().showSection(rot1, Direction.DOWN)


        scene.idle(45)

        scene.overlay().showText(50)
            .attachKeyFrame()
            .text("16 RPM")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(4, 1, 3), Direction.WEST))
        scene.idle(60)

        scene.world().showSection(lever, Direction.DOWN)
        scene.idle(5)
        scene.world().showSection(redstone, Direction.DOWN)
        scene.idle(15)

        val leverPos = util.grid().at(2, 1, 1)
        val power = RedStoneWireBlock.POWER
        val leverVec = util.vector().centerOf(leverPos).add(0.0, -.25, 0.0)
        scene.overlay().showControls(leverVec, Pointing.DOWN, 40).rightClick()

        scene.idle(45)

        scene.world().modifyBlockEntityNBT(
            lever,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }
        scene.world().modifyBlock(util.grid().at(2, 1, 2),
            { s: BlockState ->
                s.setValue(
                    power,
                    5
                )
            }, false
        )
        scene.effects().indicateRedstone(util.grid().at(2, 1, 2))
        //5/15 = 10.67tpm
        scene.world().setKineticSpeed(rot1, -10.67f)
        scene.overlay().showText(50)
            .attachKeyFrame()
            .text("Speed will go down depending on redstone level")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
        scene.idle(60)

        scene.overlay().showText(50)
            .attachKeyFrame()
            .text("10.67 RPM")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(0, 1, 3), Direction.WEST))
        scene.idle(60)

        //10/15 = 5.33rpm
        scene.world().modifyBlockEntityNBT(
            lever,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                10
            )
        }
        scene.world().modifyBlock(util.grid().at(2, 1, 2),
            { s: BlockState ->
                s.setValue(
                    power,
                    10
                )
            }, false
        )
        scene.effects().indicateRedstone(util.grid().at(2, 1, 2))
        //5/15 = 10.67tpm
        scene.world().setKineticSpeed(rot1, -5.33f)
        scene.overlay().showText(50)
            .attachKeyFrame()
            .text("5.33 RPM")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(0, 1, 3), Direction.WEST))
        scene.idle(45)

        scene.world().modifyBlockEntityNBT(
            lever,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                15
            )
        }
        scene.world().modifyBlock(util.grid().at(2, 1, 2),
            { s: BlockState ->
                s.setValue(
                    power,
                    15
                )
            }, false
        )
        scene.effects().indicateRedstone(util.grid().at(2, 1, 2))
        //5/15 = 10.67tpm
        scene.world().setKineticSpeed(rot1, 0f)
        scene.overlay().showText(50)
            .attachKeyFrame()
            .text("0 RPM")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(0, 1, 3), Direction.WEST))

        scene.idle(45)

        //scene.world().toggleRedstonePower(lever);
        //scene.world().toggleRedstonePower(redstone);
        //0,1,3   1,1,3   3,1,3   4,1,3
        scene.idle(37 * 4)
    }
}
