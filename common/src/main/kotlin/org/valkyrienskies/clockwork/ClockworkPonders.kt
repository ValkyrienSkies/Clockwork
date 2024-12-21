package org.valkyrienskies.clockwork

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity
import com.simibubi.create.foundation.ponder.PonderPalette
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper
import com.simibubi.create.foundation.ponder.SceneBuilder
import com.simibubi.create.foundation.ponder.SceneBuildingUtil
import com.simibubi.create.foundation.ponder.element.InputWindowElement
import com.simibubi.create.foundation.utility.Pointing
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity

object ClockworkPonders {

    val HELPER: PonderRegistrationHelper = PonderRegistrationHelper(ClockworkMod.MOD_ID)

    fun init() {
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
        HELPER.forComponents(ClockworkBlocks.ANDESITE_FLAP_BEARING, ClockworkBlocks.FLAP)
            .addStoryBoard(
                "flap_bearing", ::flap
            )
        HELPER.forComponents(ClockworkBlocks.GYRO).addStoryBoard(
            "gyro", ::gyro
        )
        HELPER.forComponents(ClockworkBlocks.DELIVERY_CANNON, ClockworkBlocks.DELIVERY_CHUTE)
            .addStoryBoard(
                "solid_delivery", ::solid_delivery
            )
    }

    private fun solid_delivery(scene: SceneBuilder, util: SceneBuildingUtil) {

        scene.title("solid_delivery", "Solid delivery")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)
        val depotLine = util.select.fromTo(0, 1, 0, 4, 2, 0)
        val chuteLine = util.select.fromTo(0, 1, 2, 4, 2, 2)
        val beltLine = util.select.fromTo(0, 1, 4, 4, 2, 4)

        val cannonSlot = util.vector.blockSurface(BlockPos(0,2,0), Direction.NORTH)
            .add(0.0, -0.35, 0.0)

        val chuteSlot = util.vector.blockSurface(BlockPos(4,2,0), Direction.NORTH)
            .add(0.0, -0.35, 0.0)




        scene.world.showSection(depotLine, Direction.DOWN)
        scene.overlay.showText(60)
            .attachKeyFrame()
            .text("Delivery cannons and delivery chutes allows transporting items in a short distance")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.WEST))


        scene.idle(60)
        scene.overlay.showFilterSlotInput(cannonSlot, Direction.NORTH, 60)
        scene.overlay.showFilterSlotInput(chuteSlot, Direction.NORTH, 60)
        scene.overlay.showText(120)
            .attachKeyFrame()
            .text("Delivery Cannons and Chutes have a frequency slot, similar to Redstone links. Delivery cannons will only fire to chutes with the same frequency")
            .pointAt(chuteSlot)

        scene.idle(120)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Delivery cannons will shoot out any items placed in the storage below it")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.WEST))
        scene.world.createItemOnBeltLike(BlockPos(0,1,0), Direction.NORTH, ItemStack(Items.COPPER_BLOCK))
        scene.idle(2)
        scene.world.modifyBlockEntity(BlockPos(0, 2, 0), DeliveryCannonBlockEntity::class.java) { be: DeliveryCannonBlockEntity -> be.ponder = true}
        scene.idle(2)
        scene.world.modifyBlockEntity(BlockPos(0, 2, 0), DeliveryCannonBlockEntity::class.java) { be: DeliveryCannonBlockEntity -> be.ponderFire(BlockPos(4,2,0))}
        scene.idle(60)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("They can only be placed on Depots,")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(4, 1, 0), Direction.WEST))
        scene.idle(40)
        scene.world.showSection(chuteLine, Direction.DOWN)
        scene.world.hideSection(depotLine, Direction.DOWN)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Chutes,")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(4, 1, 2), Direction.WEST))
        scene.idle(40)
        scene.world.showSection(beltLine, Direction.DOWN)
        scene.world.hideSection(chuteLine, Direction.DOWN)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("and belts")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(4, 1, 4), Direction.WEST))
        scene.idle(40)
    }

    private fun flap(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("flap_bearing", "Steering planes")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val bearing = util.select.fromTo(2, 1, 2, 2, 1, 4)
        val flap_ship = util.select.position(2, 1, 1)
        val contraption =
            scene.world.showIndependentSection(flap_ship, Direction.DOWN)
        scene.world.moveSection(contraption, util.vector.of(0.0, 0.0, 0.0), 0)
        val red1 = util.select.fromTo(0, 1, 2, 1, 1, 2)
        val red2 = util.select.fromTo(4, 1, 2, 3, 1, 2)
        scene.world.showSection(bearing, Direction.DOWN)
        scene.idle(15)
        //scene.world.showSection(flap_ship, Direction.DOWN);
        scene.idle(45)
        scene.world.showSection(red1, Direction.DOWN)
        scene.world.showSection(red2, Direction.DOWN)
        scene.idle(25)
        scene.world.toggleRedstonePower(red1)
        scene.world.rotateSection(contraption, 0.0, 0.0, 25.0, 17)
        scene.idle(35)
        scene.world.toggleRedstonePower(red1)
        scene.world.toggleRedstonePower(red2)
        scene.world.rotateSection(contraption, 0.0, 0.0, -50.0, 17)
        scene.idle(35)
        scene.idle(37 * 4)
    }

    private fun altMeter(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("alt_meter", "Measure height")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val ship = util.select.fromTo(0, 1, 0, 4, 3, 4)

        val contraption =
            scene.world.showIndependentSection(ship, Direction.DOWN)
        scene.world.moveSection(contraption, util.vector.of(0.0, 0.0, 0.0), 0)
        scene.idle(15)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Configure Altitude Meter to desired height")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.WEST))
        scene.idle(40)
        scene.world.moveSection(contraption, util.vector.of(0.0, 2.0, 0.0), 20)
        scene.idle(15)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Redstone output will trigger at configured height")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 4, 0), Direction.WEST))
        scene.world.toggleRedstonePower(ship)
        scene.idle(40)

        scene.idle(37 * 4)
    }

    private fun createShip(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("wanderwand", "Creating ships using the Wanderwand")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val lever = util.select.position(0, 0, 0)
        val ship = util.select.fromTo(0, 1, 1, 4, 3, 3)

        val contraption =
            scene.world.showIndependentSection(ship, Direction.DOWN)
        scene.world.moveSection(contraption, util.vector.of(0.0, 0.0, 0.0), 0)
        scene.idle(15)
        scene.overlay.showControls(
            InputWindowElement(util.vector.topOf(0, 2, 1), Pointing.UP)
                .withItem(ClockworkItems.WANDERWAND.asStack())
                .rightClick(),
            40
        )
        scene.idle(6)

        scene.effects.indicateSuccess(util.grid.at(0, 2, 1))

        scene.idle(45)
        scene.overlay.showControls(
            InputWindowElement(util.vector.blockSurface(util.grid.at(4, 3, 4), Direction.DOWN), Pointing.DOWN)
                .withItem(ClockworkItems.WANDERWAND.asStack())
                .rightClick(),
            40
        )
        scene.idle(6)

        val bb = AABB(util.grid.at(0, 2, 1))
        scene.overlay.chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb, 1)
        scene.overlay.chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb.expandTowards(4.0, 1.0, 2.0), 70)

        scene.idle(70)


        scene.world.setBlock(util.grid.at(0, 1, 0), ClockworkBlocks.PHYSICS_INFUSER.defaultState, false)
        scene.world.showSection(util.select.position(0, 1, 0), Direction.NORTH)
        scene.idle(20)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Place the wand in the Physics Infuser")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.WEST))
        scene.overlay.showControls(
            InputWindowElement(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.DOWN), Pointing.DOWN)
                .withItem(ClockworkItems.WANDERWAND.asStack())
                .rightClick(),
            40
        )

        scene.idle(50)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Or use the Gravitron")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(2, 2, 2), Direction.WEST))
        scene.overlay.showControls(
            InputWindowElement(util.vector.blockSurface(util.grid.at(2, 2, 2), Direction.DOWN), Pointing.DOWN)
                .withItem(ClockworkItems.CREATIVE_GRAVITRON.asStack())
                .rightClick(),
            40
        )
        scene.idle(50)

        scene.world.moveSection(contraption, util.vector.of(0.0, -0.1, 0.0), 4)
        scene.idle(4)
        scene.world.moveSection(contraption, util.vector.of(0.0, -0.1, 0.0), 3)
        scene.idle(3)
        scene.world.moveSection(contraption, util.vector.of(0.0, -0.1, 0.0), 2)
        scene.idle(2)
        scene.world.moveSection(contraption, util.vector.of(0.0, -0.7, 0.0), 10)
        scene.idle(14)
        scene.world.moveSection(contraption, util.vector.of(0.0, 0.015, 0.0), 3)
        scene.idle(3)
        scene.world.moveSection(contraption, util.vector.of(0.0, -0.010, 0.0), 2)
        scene.idle(2)
        scene.world.moveSection(contraption, util.vector.of(0.0, -0.005, 0.0), 1)


        scene.idle(20)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("This Ship is now affected by physics!")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(2, 0, 3), Direction.WEST))
        scene.idle(37 * 4)
    }

    private fun redstoneResistor(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("resistor", "Using the Redstone Resisitor")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val lever = util.select.position(2, 1, 1)
        val resistor = util.select.position(2, 1, 3)
        val redstone = util.select.position(2, 1, 2)
        val rot1 = util.select.fromTo(0, 1, 3, 1, 1, 3)
        val rot2 = util.select.fromTo(3, 1, 3, 4, 1, 3)

        scene.world.showSection(rot2, Direction.DOWN)
        scene.idle(5)
        scene.world.showSection(resistor, Direction.DOWN)
        scene.idle(5)
        scene.world.showSection(rot1, Direction.DOWN)


        scene.idle(45)

        scene.overlay.showText(50)
            .attachKeyFrame()
            .text("16 RPM")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(4, 1, 3), Direction.WEST))
        scene.idle(60)

        scene.world.showSection(lever, Direction.DOWN)
        scene.idle(5)
        scene.world.showSection(redstone, Direction.DOWN)
        scene.idle(15)

        val leverPos = util.grid.at(2, 1, 1)
        val power = RedStoneWireBlock.POWER
        val leverVec = util.vector.centerOf(leverPos).add(0.0, -.25, 0.0)
        scene.overlay.showControls(InputWindowElement(leverVec, Pointing.DOWN).rightClick(), 40)

        scene.idle(45)

        scene.world.modifyBlockEntityNBT(
            lever,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }
        scene.world.modifyBlock(util.grid.at(2, 1, 2),
            { s: BlockState ->
                s.setValue(
                    power,
                    5
                )
            }, false
        )
        scene.effects.indicateRedstone(util.grid.at(2, 1, 2))
        //5/15 = 10.67tpm
        scene.world.setKineticSpeed(rot1, -10.67f)
        scene.overlay.showText(50)
            .attachKeyFrame()
            .text("Speed will go down depending on redstone level")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(2, 1, 2), Direction.WEST))
        scene.idle(60)

        scene.overlay.showText(50)
            .attachKeyFrame()
            .text("10.67 RPM")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 1, 3), Direction.WEST))
        scene.idle(60)

        //10/15 = 5.33rpm
        scene.world.modifyBlockEntityNBT(
            lever,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                10
            )
        }
        scene.world.modifyBlock(util.grid.at(2, 1, 2),
            { s: BlockState ->
                s.setValue(
                    power,
                    10
                )
            }, false
        )
        scene.effects.indicateRedstone(util.grid.at(2, 1, 2))
        //5/15 = 10.67tpm
        scene.world.setKineticSpeed(rot1, -5.33f)
        scene.overlay.showText(50)
            .attachKeyFrame()
            .text("5.33 RPM")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 1, 3), Direction.WEST))
        scene.idle(45)

        scene.world.modifyBlockEntityNBT(
            lever,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                15
            )
        }
        scene.world.modifyBlock(util.grid.at(2, 1, 2),
            { s: BlockState ->
                s.setValue(
                    power,
                    15
                )
            }, false
        )
        scene.effects.indicateRedstone(util.grid.at(2, 1, 2))
        //5/15 = 10.67tpm
        scene.world.setKineticSpeed(rot1, 0f)
        scene.overlay.showText(50)
            .attachKeyFrame()
            .text("0 RPM")
            .placeNearTarget()
            .pointAt(util.vector.blockSurface(util.grid.at(0, 1, 3), Direction.WEST))

        scene.idle(45)

        //scene.world.toggleRedstonePower(lever);
        //scene.world.toggleRedstonePower(redstone);
        //0,1,3   1,1,3   3,1,3   4,1,3
        scene.idle(37 * 4)
    }

    private fun gyro(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("gyro", "Stabilize your ship")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.setSceneOffsetY(-1f)
        scene.idle(15)

        val leverN = util.select.position(2, 2, 3)
        val leverE = util.select.position(3, 2, 2)
        val leverS = util.select.position(2, 2, 1)
        val leverW = util.select.position(1, 2, 2)
        val ship = util.select.fromTo(0, 1, 0, 4, 3, 4)

        val contraption = scene.world.showIndependentSection(ship, Direction.DOWN)
        scene.world.moveSection(contraption, util.vector.of(0.0, 0.0, 0.0), 0)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Gyro will stabilize ship in its direction")
        scene.idle(40)
        scene.idle(20)
        scene.overlay.showText(40)
            .attachKeyFrame()
            .text("Redstone input from each side will tilt the ship")
        scene.idle(30)
        scene.world.modifyBlockEntityNBT(
            leverN,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }
        scene.world.configureCenterOfRotation(contraption, Vec3(2.0, 2.0, 2.0))
        scene.world.rotateSection(contraption, -25.0, 0.0, 0.0, 30)
        scene.idle(40)
        scene.world.modifyBlockEntityNBT(
            leverN,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }
        scene.world.rotateSection(contraption, 25.0, 0.0, 0.0, 30)
        scene.idle(40)

        scene.world.modifyBlockEntityNBT(
            leverE,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                5
            )
        }
        scene.world.rotateSection(contraption, 0.0, 0.0, 25.0, 30)
        scene.idle(40)
        scene.world.modifyBlockEntityNBT(
            leverE,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }
        scene.world.rotateSection(contraption, 0.0, 0.0, -25.0, 30)
        scene.idle(40)

        scene.world.modifyBlockEntityNBT(
            leverW,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                10
            )
        }
        scene.world.rotateSection(contraption, 0.0, 0.0, -50.0, 50)
        scene.idle(60)
        scene.world.modifyBlockEntityNBT(
            leverW,
            AnalogLeverBlockEntity::class.java
        ) { nbt: CompoundTag ->
            nbt.putInt(
                "State",
                0
            )
        }
        scene.world.rotateSection(contraption, 0.0, 0.0, 50.0, 50)
        scene.idle(40)

        scene.idle(37 * 4)
    }
}