package org.valkyrienskies.clockwork;

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.*;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ClockworkPonderScenes {
    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(ClockworkMod.MOD_ID);

    public static void init() {
        HELPER.forComponents(ClockworkItems.AURIC_DESIGNATOR, ClockworkBlocks.PHYSICS_INFUSER)
                .addStoryBoard("auric_designator", ClockworkPonderScenes::createShip);
        HELPER.forComponents(ClockworkBlocks.REDSTONE_RESISTOR)
                .addStoryBoard("resistor", ClockworkPonderScenes::redstoneResistor);
        HELPER.forComponents(ClockworkBlocks.ALT_METER)
                .addStoryBoard("alt_meter", ClockworkPonderScenes::altMeter);
        HELPER.forComponents(ClockworkBlocks.FLAP_BEARING, ClockworkBlocks.FLAP)
                .addStoryBoard("flap_bearing", ClockworkPonderScenes::flap);
    }

    private static void flap(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("flap_bearing", "Steering planes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1);
        scene.idle(15);

        Selection bearing = util.select.fromTo(2, 1, 2, 2, 1, 4);
        Selection flap_ship = util.select.position(2, 1, 1);
        ElementLink<WorldSectionElement> contraption =
                scene.world.showIndependentSection(flap_ship, Direction.DOWN);
        scene.world.moveSection(contraption, util.vector.of(0, 0, 0), 0);
        Selection red1 = util.select.fromTo(0,1,2, 1,1,2);
        Selection red2 = util.select.fromTo(4, 1, 2, 3, 1, 2);
        scene.world.showSection(bearing, Direction.DOWN);
        scene.idle(15);
        //scene.world.showSection(flap_ship, Direction.DOWN);
        scene.idle(45);
        scene.world.showSection(red1, Direction.DOWN);
        scene.world.showSection(red2, Direction.DOWN);
        scene.idle(25);
        scene.world.toggleRedstonePower(red1);
        scene.world.rotateSection(contraption, 0.0,0.0,25, 17);
        scene.idle(35);
        scene.world.toggleRedstonePower(red1);
        scene.world.toggleRedstonePower(red2);
        scene.world.rotateSection(contraption, 0.0,0.0,-50, 17);
        scene.idle(35);
        scene.idle(37 * 4);
    }

    private static void altMeter(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("alt_meter", "Measure height");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1);
        scene.idle(15);

        Selection ship = util.select.fromTo(0, 1, 0, 4, 3, 4);

        ElementLink<WorldSectionElement> contraption =
                scene.world.showIndependentSection(ship, Direction.DOWN);
        scene.world.moveSection(contraption, util.vector.of(0, 0, 0), 0);
        scene.idle(15);
        scene.overlay.showText(40)
                .attachKeyFrame()
                .text("Configure Altitude Meter to desired height")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.WEST));
        scene.idle(40);
        scene.world.moveSection(contraption, util.vector.of(0, 2, 0), 20);
        scene.idle(15);
        scene.overlay.showText(40)
                .attachKeyFrame()
                .text("Redstone output will trigger at configured height")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(0, 4, 0), Direction.WEST));
        scene.world.toggleRedstonePower(ship);
        scene.idle(40);

        scene.idle(37 * 4);
    }

    private static void createShip(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("auric_designator", "Creating ships using Auric Designator");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1);
        scene.idle(15);

        Selection lever = util.select.position(0, 0, 0);
        Selection ship = util.select.fromTo(0, 1, 1, 4, 3, 3);

        ElementLink<WorldSectionElement> contraption =
                scene.world.showIndependentSection(ship, Direction.DOWN);
        scene.world.moveSection(contraption, util.vector.of(0, 0, 0), 0);
        scene.idle(15);
        scene.overlay.showControls(
                new InputWindowElement(util.vector.topOf(0, 2, 1), Pointing.UP)
                        .withItem(ClockworkItems.AURIC_DESIGNATOR.asStack())
                        .rightClick(),
                40);
        scene.idle(6);

        scene.effects.indicateSuccess(util.grid.at(0, 2, 1));

        scene.idle(45);
        scene.overlay.showControls(
                new InputWindowElement(util.vector.blockSurface(util.grid.at(4, 3, 4), Direction.DOWN), Pointing.DOWN)
                        .withItem(ClockworkItems.AURIC_DESIGNATOR.asStack())
                        .rightClick(),
                40);
        scene.idle(6);

        AABB bb = new AABB(util.grid.at(0, 2, 1));
        scene.overlay.chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb, 1);
        scene.overlay.chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb.expandTowards(4, 1, 2), 70);

        scene.idle(70);


        scene.world.setBlock(util.grid.at(0, 1, 0), ClockworkBlocks.PHYSICS_INFUSER.getDefaultState(), false);
        scene.world.showSection(util.select.position(0,1,0), Direction.NORTH);
        scene.idle(20);
        scene.overlay.showText(40)
                .attachKeyFrame()
                .text("Place the Auric Designator in the Physics Infuser")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.WEST));
        scene.overlay.showControls(
                new InputWindowElement(util.vector.blockSurface(util.grid.at(0, 2, 0), Direction.DOWN), Pointing.DOWN)
                        .withItem(ClockworkItems.AURIC_DESIGNATOR.asStack())
                        .rightClick(),
                40);

        scene.idle(50);
        scene.overlay.showText(40)
                .attachKeyFrame()
                .text("Or use the Gravitron")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(2, 2, 2), Direction.WEST));
        scene.overlay.showControls(
                new InputWindowElement(util.vector.blockSurface(util.grid.at(2, 2, 2), Direction.DOWN), Pointing.DOWN)
                        .withItem(ClockworkItems.GRAVITRON.asStack())
                        .rightClick(),
                40);
        scene.idle(50);

        scene.world.moveSection(contraption, util.vector.of(0, -0.1, 0), 4);
        scene.idle(4);
        scene.world.moveSection(contraption, util.vector.of(0, -0.1, 0), 3);
        scene.idle(3);
        scene.world.moveSection(contraption, util.vector.of(0, -0.1, 0), 2);
        scene.idle(2);
        scene.world.moveSection(contraption, util.vector.of(0, -0.7, 0), 10);
        scene.idle(14);
        scene.world.moveSection(contraption, util.vector.of(0, 0.015, 0), 3);
        scene.idle(3);
        scene.world.moveSection(contraption, util.vector.of(0, -0.010, 0), 2);
        scene.idle(2);
        scene.world.moveSection(contraption, util.vector.of(0, -0.005, 0), 1);


        scene.idle(20);
        scene.overlay.showText(40)
                .attachKeyFrame()
                .text("This Ship is now affected by physics!")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(2, 0, 3), Direction.WEST));
        scene.idle(37 * 4);
    }

    private static void redstoneResistor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("resistor", "Using the Redstone Resisitor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1);
        scene.idle(15);

        Selection lever = util.select.position(2, 1, 1);
        Selection resistor = util.select.position(2, 1, 3);
        Selection redstone = util.select.position(2, 1, 2);
        Selection rot1 = util.select.fromTo(0, 1, 3, 1, 1, 3);
        Selection rot2 = util.select.fromTo(3, 1, 3, 4, 1, 3);

        scene.world.showSection(rot2, Direction.DOWN);
        scene.idle(5);
        scene.world.showSection(resistor, Direction.DOWN);
        scene.idle(5);
        scene.world.showSection(rot1, Direction.DOWN);


        scene.idle(45);

        scene.overlay.showText(50)
                .attachKeyFrame()
                .text("16 RPM")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(4, 1, 3), Direction.WEST));
        scene.idle(60);

        scene.world.showSection(lever, Direction.DOWN);
        scene.idle(5);
        scene.world.showSection(redstone, Direction.DOWN);
        scene.idle(15);

        BlockPos leverPos = util.grid.at(2, 1, 1);
        IntegerProperty power = RedStoneWireBlock.POWER;
        Vec3 leverVec = util.vector.centerOf(leverPos).add(0, -.25, 0);
        scene.overlay.showControls(new InputWindowElement(leverVec, Pointing.DOWN).rightClick(), 40);

        scene.idle(45);

        scene.world.modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 5));
        scene.world.modifyBlock(util.grid.at(2, 1, 2), s -> s.setValue(power, 5), false);
        scene.effects.indicateRedstone(util.grid.at(2, 1, 2));
        //5/15 = 10.67tpm
        scene.world.setKineticSpeed(rot1, -10.67f);
        scene.overlay.showText(50)
                .attachKeyFrame()
                .text("Speed will go down depending on redstone level")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(2, 1, 2), Direction.WEST));
        scene.idle(60);

        scene.overlay.showText(50)
                .attachKeyFrame()
                .text("10.67 RPM")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(0, 1, 3), Direction.WEST));
        scene.idle(60);

        //10/15 = 5.33rpm

        scene.world.modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 10));
        scene.world.modifyBlock(util.grid.at(2, 1, 2), s -> s.setValue(power, 10), false);
        scene.effects.indicateRedstone(util.grid.at(2, 1, 2));
        //5/15 = 10.67tpm
        scene.world.setKineticSpeed(rot1, -5.33f);
        scene.overlay.showText(50)
                .attachKeyFrame()
                .text("5.33 RPM")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(0, 1, 3), Direction.WEST));
        scene.idle(45);

        scene.world.modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 15));
        scene.world.modifyBlock(util.grid.at(2, 1, 2), s -> s.setValue(power, 15), false);
        scene.effects.indicateRedstone(util.grid.at(2, 1, 2));
        //5/15 = 10.67tpm
        scene.world.setKineticSpeed(rot1, 0);
        scene.overlay.showText(50)
                .attachKeyFrame()
                .text("0 RPM")
                .placeNearTarget()
                .pointAt(util.vector.blockSurface(util.grid.at(0, 1, 3), Direction.WEST));

        scene.idle(45);

        //scene.world.toggleRedstonePower(lever);
        //scene.world.toggleRedstonePower(redstone);
        //0,1,3   1,1,3   3,1,3   4,1,3
        scene.idle(37 * 4);
    }
}
