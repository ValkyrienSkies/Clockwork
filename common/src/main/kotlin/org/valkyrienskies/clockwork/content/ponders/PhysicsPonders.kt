package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.ponders.moveSectionAsShip
import org.valkyrienskies.clockwork.ponderLang
import org.valkyrienskies.clockwork.sparklingPlane

object PhysicsPonders {
    fun flap(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)

        scene.title("flap_bearing", "Flap bearing")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val flap_bearing = BlockPos(2, 1, 2)
        val flap_selection = util.select().position(2, 1, 1)

        val bottom_cog = BlockPos(2, 0, 5)
        val shaft_selection = util.select().fromTo(flap_bearing, BlockPos(2, 1, 5))

        val red1 = util.select().fromTo(0, 1, 2, 1, 1, 2)
        val red2 = util.select().fromTo(4, 1, 2, 3, 1, 2)

        // region Setup flap contraption
        val contraption =
            scene.world().showIndependentSection(flap_selection, Direction.DOWN)
        // endregion

        // region Show everything except redstone+flap
        scene.world().showSection(util.select().layersFrom(1).substract(red1).substract(red2).substract(flap_selection), Direction.DOWN)
        scene.world().showSection(shaft_selection, Direction.DOWN)
        scene.world().showSection(util.select().position(bottom_cog), Direction.DOWN)
        scene.idle(20)
        // endregion

        scene.world().setKineticSpeed(shaft_selection, 16.0F)
        scene.world().setKineticSpeed(util.select().position(bottom_cog), -16.0F)

        scene.overlay().showText(60)
            .text(scene.ponderLang(1))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))
        scene.idle(60+20)

        scene.overlay().showText(60)
            .text(scene.ponderLang(2))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))
        scene.idle(60+20)

        scene.overlay().showText(60)
            .text(scene.ponderLang(3))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))
        scene.idle(60+20)

        scene.addKeyframe()

        scene.world().showSection(red1, Direction.DOWN)
        scene.world().showSection(red2, Direction.DOWN)
        scene.idle(15)

        scene.overlay().showText(60)
            .text(scene.ponderLang(4))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))
        scene.idle(60+20)

        // region Tilt towards red1
        scene.world().toggleRedstonePower(red1)
        scene.world().rotateSection(contraption, 0.0, 0.0, 25.0, 6)

        scene.world().modifyBlockEntityNBT(
            util.select().position(flap_bearing),
            FlapBearingBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putFloat("TargetAngle", 22.5F) }

        scene.idle(35)
        // endregion

        // region Tilt towards red2
        scene.world().toggleRedstonePower(red1)
        scene.world().toggleRedstonePower(red2)
        scene.world().modifyBlockEntityNBT(
            util.select().position(flap_bearing),
            FlapBearingBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putFloat("TargetAngle", -22.5F) }

        scene.world().rotateSection(contraption, 0.0, 0.0, -50.0, 6*2)
        scene.idle(35)
        // endregion

        // region Reset rotation
        scene.world().toggleRedstonePower(red2)
        scene.world().modifyBlockEntityNBT(
            util.select().position(flap_bearing),
            FlapBearingBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putFloat("TargetAngle", 0.0F) }

        scene.world().rotateSection(contraption, 0.0, 0.0, 25.0, 6)

        scene.idle(40)

        // endregion

        scene.addKeyframe()

        scene.idle(20)

        // region Swap out levers for analog levers
        scene.world().hideSection(red1, Direction.UP)
        scene.world().hideSection(red2, Direction.UP)
        scene.idle(20)
        scene.world().setBlock(BlockPos(4, 1, 2), AllBlocks.ANALOG_LEVER.defaultState, false)
        scene.world().setBlock(BlockPos(0, 1, 2), AllBlocks.ANALOG_LEVER.defaultState, false)
        scene.world().showSection(red1, Direction.DOWN)
        scene.world().showSection(red2, Direction.DOWN)
        scene.idle(15)
        // endregion

        val leverPos = BlockPos(0, 1, 2)

        scene.overlay().showText(80)
            .text(scene.ponderLang(5))
            .pointAt(util.vector().centerOf(leverPos))
        scene.idle(80+20)

        // region Analogue tilt to red2

        // "Click" lever
        scene.overlay().showControls(
            util.vector().centerOf(leverPos),
            Pointing.DOWN,
            20
        )
            .rightClick()
        scene.idle(10)

        // Set analog lever strength
        scene.world().modifyBlockEntityNBT(
            util.select().position(leverPos),
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("State", 3) }
        scene.idle(10)

        // Set redstone dust strength
        scene.world().modifyBlock(
            leverPos.east(),
            { s: BlockState? -> s!!.setValue(RedStoneWireBlock.POWER, 3) },
            false
        )
        scene.effects().indicateRedstone(leverPos.east())

        // Rotate bearing
        scene.world().rotateSection(contraption, 0.0, 0.0, 4.5, 1)
        scene.world().modifyBlockEntityNBT(
            util.select().position(flap_bearing),
            FlapBearingBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putFloat("TargetAngle", 4.5F) }
        scene.idle(20)

        // endregion

        // region Analogue tilt to red2 more

        // "Click" lever
        scene.overlay().showControls(
            util.vector().centerOf(leverPos),
            Pointing.DOWN,
            20
        )
            .rightClick()
        scene.idle(10)

        // Set analog lever strength
        scene.world().modifyBlockEntityNBT(
            util.select().position(leverPos),
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("State", 7) }
        scene.idle(10)

        // Set redstone dust strength
        scene.world().modifyBlock(
            leverPos.east(),
            { s: BlockState? -> s!!.setValue(RedStoneWireBlock.POWER, 7) },
            false
        )
        scene.effects().indicateRedstone(leverPos.east())

        // Rotate bearing
        scene.world().rotateSection(contraption, 0.0, 0.0, 10.5, 3)
        scene.world().modifyBlockEntityNBT(
            util.select().position(flap_bearing),
            FlapBearingBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putFloat("TargetAngle", 10.5F) }
        scene.idle(20)

        // endregion
        scene.idle(20)

        scene.markAsFinished()

    }

    fun smart_flap(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)

        scene.title("smart_flap_bearing", "Smart flap bearing")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val flap_bearing = BlockPos(2, 1, 2)
        val flap_selection = util.select().position(2, 1, 1)

        val bottom_cog = BlockPos(2, 0, 5)
        val shaft_selection = util.select().fromTo(flap_bearing, BlockPos(2, 1, 5))

        val red1 = util.select().fromTo(0, 1, 2, 1, 1, 2)
        val red2 = util.select().fromTo(4, 1, 2, 3, 1, 2)

        // region Setup flap contraption
        val contraption =
            scene.world().showIndependentSection(flap_selection, Direction.DOWN)
        // endregion

        // region Show everything except redstone+flap
        scene.world().showSection(util.select().layersFrom(1).substract(red1).substract(red2).substract(flap_selection), Direction.DOWN)
        scene.world().showSection(shaft_selection, Direction.DOWN)
        scene.world().showSection(util.select().position(bottom_cog), Direction.DOWN)
        scene.idle(20)
        // endregion

        scene.world().setKineticSpeed(shaft_selection, 16.0F)
        scene.world().setKineticSpeed(util.select().position(bottom_cog), -16.0F)

        scene.overlay().showText(60)
            .text(scene.ponderLang(1))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))
        scene.idle(60+20)

        scene.overlay().showText(60)
            .attachKeyFrame()
            .text(scene.ponderLang(2))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))

        val slot1 = util.vector().blockSurface(flap_bearing, Direction.WEST)
            .add(0.0, 0.2, 0.1)
        val slot2 = util.vector().blockSurface(flap_bearing, Direction.WEST)
            .subtract(0.0, 0.2, -0.1)

        scene.overlay().showFilterSlotInput(slot1, Direction.WEST, 60)
        scene.overlay().showFilterSlotInput(slot2, Direction.WEST, 60)

        scene.idle(60+20)

        scene.overlay().showText(60)
            .text(scene.ponderLang(3))
            .pointAt(util.vector().blockSurface(flap_bearing, Direction.WEST))
        scene.idle(60+20)

        scene.markAsFinished()
    }

    fun altMeter(scene: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(scene)
        scene.title("alt_meter", "Altitude Meter")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val ship = util.select().fromTo(1, 1, 2, 3, 2, 2)
        val lamp = util.select().position(3, 2, 2)
        val nixie = util.select().position(1,2,2)
        val meter = BlockPos(2, 2, 2)

        scene.idle(30)

        //region Show altitude setting
        var contraption = scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.idle(15)

        scene.overlay().showText(60)
            .text(scene.ponderLang(1))
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(60+20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.2, 0.0))
        scene.idle(20-10)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 15) }
        scene.idle(20)

        scene.sparklingPlane(AABB(-1.0, 4.0, -1.0, 5.0, 4.0, 5.0), PonderPalette.GREEN, Direction.UP, 60)

        scene.overlay().showText(60)
            .text(scene.ponderLang(2))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(60+20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.2, 0.0))
        scene.idle(20-10)

        scene.sparklingPlane(AABB(-1.0, 4.0, -1.0, 5.0, 8.0, 5.0), PonderPalette.GREEN, Direction.UP, 60)

        scene.overlay().showText(60)
            .text(scene.ponderLang(3))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(70)
        // endregion

        scene.world().hideIndependentSection(contraption, Direction.DOWN)
        scene.idle(20)
        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 0) }

        // region Show sensitivity setting
        scene.addKeyframe()

        contraption = scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.idle(10)

        scene.overlay().showText(120)
            .text(scene.ponderLang(4))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(120+20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 7) }
        scene.idle(20)

        scene.sparklingPlane(AABB(0.0, 0.0, 0.0, 5.0, 4.0, 5.0), PonderPalette.GREEN, Direction.DOWN, 90+20+10+30)

        scene.overlay().showText(90)
            .text(scene.ponderLang(5))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(90+20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 15) }
        scene.idle(20)
        // endregion

        scene.world().hideIndependentSection(contraption, Direction.DOWN)
        scene.idle(20)
        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 0) }

        // region Explain direction setting
        scene.addKeyframe()

        contraption = scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.idle(10)

        scene.overlay().showText(90)
            .text(scene.ponderLang(6))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(90+20)

        scene.overlay().showText(80)
            .text(scene.ponderLang(7))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(80+20)
        // endregion

        // region Show UP direction
        scene.addKeyframe()

        scene.overlay().showText(90)
            .text(scene.ponderLang(8))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(90+20)

        scene.sparklingPlane(AABB(0.0, 0.0, 0.0, 5.0, 4.0, 5.0), PonderPalette.GREEN, Direction.DOWN, 80)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 7) }
        scene.idle(20)

        scene.world().moveSectionAsShip(scene, contraption,  20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 15) }
        scene.idle(20)
        // endregion

        scene.world().hideIndependentSection(contraption, Direction.UP)
        scene.idle(20)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 0) }

        // region Show DOWN direction
        scene.addKeyframe()

        contraption = scene.world().showIndependentSectionImmediately(ship)
        // Move up to where we used to be before disappearing
        scene.world().moveSection(contraption, Vec3(0.0, 3.0, 0.0), 0)
        // Visually move up higher
        scene.world().moveSection(contraption, Vec3(0.0, 1.0, 0.0),  10)

        scene.overlay().showText(80)
            .text(scene.ponderLang(9))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(BlockPos(2, 6, 2), Direction.SOUTH))

        scene.idle(80+20)

        scene.sparklingPlane(AABB(0.0, 3.0, 0.0, 5.0, 6.0, 5.0), PonderPalette.GREEN, Direction.UP, 80)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, -0.1, 0.0))
        scene.idle(20-10)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 7) }
        scene.idle(20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, -0.1, 0.0))
        scene.idle(20-10)

        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 15) }
        scene.idle(20)
        // endregion

        scene.world().hideIndependentSection(contraption, Direction.DOWN)
        scene.idle(20)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 0) }

        //region Show BOTH direction
        scene.addKeyframe()

        contraption = scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.idle(10)

        scene.overlay().showText(80)
            .text(scene.ponderLang(10))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(80+20)

        scene.sparklingPlane(AABB(-1.0, 4.0, -1.0, 5.0, 8.0, 5.0), PonderPalette.GREEN, Direction.UP, 300)
        scene.sparklingPlane(AABB(-1.0, 0.0, -1.0, 5.0, 4.0, 5.0), PonderPalette.GREEN, Direction.DOWN, 300)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 7) }
        scene.idle(20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 15) }
        scene.idle(20)

        scene.overlay().showText(80)
            .text(scene.ponderLang(11))
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(meter, Direction.SOUTH))
        scene.idle(80+20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 7) }
        scene.idle(20)

        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20-10)

        scene.world().toggleRedstonePower(lamp)
        scene.world().modifyBlockEntityNBT(
            nixie,
            NixieTubeBlockEntity::class.java
        ) { nbt: CompoundTag? -> nbt!!.putInt("RedstoneStrength", 0) }
        scene.idle(20)
        //end region

        scene.markAsFinished()
    }

    fun createShip(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)
        scene.title("wanderwand", "Creating ships using the Wanderwand")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val lever = util.select().position(0, 0, 0)
        val ship = util.select().fromTo(0, 1, 1, 4, 3, 3)

        val contraption =
            scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.world().moveSection(contraption, util.vector().of(0.0, 0.0, 0.0), 0)
        scene.idle(15)
        scene.overlay().showControls(util.vector().topOf(0, 2, 1), Pointing.UP, 40)
            .withItem(ClockworkItems.WANDERWAND.asStack())
            .rightClick()
        scene.idle(6)

        scene.effects().indicateSuccess(util.grid().at(0, 2, 1))

        scene.idle(45)
        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(4, 3, 4), Direction.DOWN), Pointing.DOWN, 40)
            .withItem(ClockworkItems.WANDERWAND.asStack())
            .rightClick()

        scene.idle(6)

        val bb = AABB(util.grid().at(0, 2, 1))
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb, 1)
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb.expandTowards(4.0, 1.0, 2.0), 70)

        scene.idle(70)


        scene.world().setBlock(util.grid().at(0, 1, 0), ClockworkBlocks.PHYSICS_INFUSER.defaultState, false)
        scene.world().showSection(util.select().position(0, 1, 0), Direction.NORTH)
        scene.idle(20)
        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Place the wand in the Physics Infuser")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(0, 2, 0), Direction.WEST))
        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(0, 2, 0), Direction.DOWN), Pointing.DOWN, 40)
            .withItem(ClockworkItems.WANDERWAND.asStack())
            .rightClick()

        scene.idle(50)
        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Or use the Gravitron")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST))
        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.DOWN), Pointing.DOWN, 40)
            .withItem(ClockworkItems.CREATIVE_GRAVITRON.asStack())
            .rightClick()

        scene.idle(50)

        scene.world().moveSection(contraption, util.vector().of(0.0, -0.1, 0.0), 4)
        scene.idle(4)
        scene.world().moveSection(contraption, util.vector().of(0.0, -0.1, 0.0), 3)
        scene.idle(3)
        scene.world().moveSection(contraption, util.vector().of(0.0, -0.1, 0.0), 2)
        scene.idle(2)
        scene.world().moveSection(contraption, util.vector().of(0.0, -0.7, 0.0), 10)
        scene.idle(14)
        scene.world().moveSection(contraption, util.vector().of(0.0, 0.015, 0.0), 3)
        scene.idle(3)
        scene.world().moveSection(contraption, util.vector().of(0.0, -0.010, 0.0), 2)
        scene.idle(2)
        scene.world().moveSection(contraption, util.vector().of(0.0, -0.005, 0.0), 1)


        scene.idle(20)
        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("This Ship is now affected by physics!")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(2, 0, 3), Direction.WEST))
        scene.idle(37 * 4)
    }

    fun gyro(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(sceneBuilder)

        scene.title("gyro", "Stabilize your ship")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val leverN = util.select().position(2, 2, 3)
        val leverE = util.select().position(3, 2, 2)
        val leverS = util.select().position(2, 2, 1)
        val leverW = util.select().position(1, 2, 2)
        val ship = util.select().fromTo(0, 1, 0, 4, 3, 4)

        val contraption = scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.world().moveSection(contraption, util.vector().of(0.0, 0.0, 0.0), 0)

        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Gyro will stabilize ship in its direction")
        scene.idle(40+20)

        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Redstone input from each side will tilt the ship")
        scene.idle(40+20)

        scene.world().configureCenterOfRotation(contraption, Vec3(2.0, 3.0, 2.0))
        scene.world().moveSectionAsShip(scene, contraption, 20, Vec3(0.0, 0.1, 0.0))
        scene.idle(20+5)

        scene.world().modifyBlockEntityNBT(
            leverN,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }

        scene.world().moveSectionAsShip(scene, contraption, 30, initialRotVel = Vec3(-2.5, 0.0, 0.0))
        scene.idle(30+10)

        scene.world().modifyBlockEntityNBT(
            leverN,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }

        scene.world().moveSectionAsShip(scene, contraption, 30, initialRotVel = Vec3(2.5, 0.0, 0.0))
        scene.idle(40)

        scene.world().modifyBlockEntityNBT(
            leverE,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }

        scene.world().moveSectionAsShip(scene, contraption, 30, initialRotVel = Vec3(0.0, 0.0, 2.5))
        scene.idle(40)

        scene.world().modifyBlockEntityNBT(
            leverE,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }

        scene.world().moveSectionAsShip(scene, contraption, 30, initialRotVel = Vec3(0.0, 0.0, -2.5))
        scene.idle(40)

        scene.world().modifyBlockEntityNBT(
            leverW,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                10
            )
        }

        scene.world().moveSectionAsShip(scene, contraption, 50, initialRotVel = Vec3(0.0, 0.0, -5.0))
        scene.idle(60)

        scene.world().modifyBlockEntityNBT(
            leverW,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }

        scene.world().moveSectionAsShip(scene, contraption, 50, initialRotVel = Vec3(0.0, 0.0, 5.0))
        scene.idle(60)

        scene.idle(20)

        scene.markAsFinished()
    }
}
