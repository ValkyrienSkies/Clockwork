package org.valkyrienskies.clockwork

import com.simibubi.create.AllBlocks
import com.simibubi.create.AllItems
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import com.tterrag.registrate.util.entry.ItemProviderEntry
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.createmod.ponder.foundation.PonderSceneBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import java.time.Clock

object ClockworkPonders {

    fun init(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        val HELPER: PonderSceneRegistrationHelper<ItemProviderEntry<*>> = helper.withKeyFunction { it.id }
        HELPER.forComponents(ClockworkItems.WANDERWAND, ClockworkBlocks.PHYSICS_INFUSER)
            .addStoryBoard(
                "wanderwand", ::createShip
            )
        HELPER.forComponents(ClockworkBlocks.REDSTONE_RESISTOR)
            .addStoryBoard(
                "resistor", ::redstoneResistor
            )

        HELPER.forComponents(ClockworkBlocks.ALT_METER)
            .addStoryBoard(
                "alt_meter", ::altMeter
            )
        HELPER.forComponents(ClockworkBlocks.ANDESITE_FLAP_BEARING, ClockworkBlocks.SMART_FLAP_BEARING, ClockworkBlocks.FLAP)
            .addStoryBoard(
                "flap_bearing", ::flap
            )
            .addStoryBoard(
                "smart_flap_bearing", ::smart_flap
            )
        HELPER.forComponents(ClockworkBlocks.GYRO).addStoryBoard(
            "gyro", ::gyro
        )
        HELPER.forComponents(ClockworkBlocks.DELIVERY_CANNON, ClockworkBlocks.DELIVERY_CHUTE)
            .addStoryBoard(
                "solid_delivery", ::solid_delivery
            )
    }

    private fun PonderSceneBuilder.ponderLang(index: Int): String {
        return Component.translatable("${ClockworkMod.MOD_ID}.ponder.${this.scene.id.path}.text_$index").string
    }

    private fun solid_delivery(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
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

    private fun flap(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
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

    private fun smart_flap(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
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

    private fun altMeter(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("alt_meter", "Measure height")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val ship = util.select().fromTo(0, 1, 0, 4, 3, 4)

        val contraption =
            scene.world().showIndependentSection(ship, Direction.DOWN)
        scene.world().moveSection(contraption, util.vector().of(0.0, 0.0, 0.0), 0)
        scene.idle(15)
        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Configure Altitude Meter to desired height")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(0, 2, 0), Direction.WEST))
        scene.idle(40)
        scene.world().moveSection(contraption, util.vector().of(0.0, 2.0, 0.0), 20)
        scene.idle(15)
        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Redstone output will trigger at configured height")
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(0, 4, 0), Direction.WEST))
        scene.world().toggleRedstonePower(ship)
        scene.idle(40)

        scene.idle(37 * 4)
    }

    private fun createShip(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
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

    private fun redstoneResistor(sceneBuilder: SceneBuilder, util: SceneBuildingUtil) {
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

    private fun gyro(scene: SceneBuilder, util: SceneBuildingUtil) {
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
        scene.idle(40)
        scene.idle(20)
        scene.overlay().showText(40)
            .attachKeyFrame()
            .text("Redstone input from each side will tilt the ship")
        scene.idle(30)
        scene.world().modifyBlockEntityNBT(
            leverN,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }
        scene.world().configureCenterOfRotation(contraption, Vec3(2.0, 2.0, 2.0))
        scene.world().rotateSection(contraption, -25.0, 0.0, 0.0, 30)
        scene.idle(40)
        scene.world().modifyBlockEntityNBT(
            leverN,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }
        scene.world().rotateSection(contraption, 25.0, 0.0, 0.0, 30)
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
        scene.world().rotateSection(contraption, 0.0, 0.0, 25.0, 30)
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
        scene.world().rotateSection(contraption, 0.0, 0.0, -25.0, 30)
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
        scene.world().rotateSection(contraption, 0.0, 0.0, -50.0, 50)
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
        scene.world().rotateSection(contraption, 0.0, 0.0, 50.0, 50)
        scene.idle(40)

        scene.idle(37 * 4)
    }
}
