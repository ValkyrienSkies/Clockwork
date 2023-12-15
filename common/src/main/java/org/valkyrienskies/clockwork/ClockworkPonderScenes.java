package org.valkyrienskies.clockwork;

import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.*;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
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
        HELPER.forComponents(ClockworkItems.BLUPERGLUE)
                .addStoryBoard("bluper_glue", ClockworkPonderScenes::createShip);
        HELPER.forComponents(ClockworkBlocks.REDSTONE_RESISTOR)
                .addStoryBoard("resistor", ClockworkPonderScenes::redstoneResistor);
    }

    private static void createShip(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("bluper_glue", "Creating ships using Bluper Glue");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1);
        scene.idle(15);

        Selection lever = util.select.position(0, 0, 0);

        scene.world.showSection(util.select.fromTo(0, 1, 0, 4, 1, 3), Direction.DOWN);
        scene.world.showSection(util.select.fromTo(0, 2, 0, 4, 2, 3), Direction.DOWN);
        scene.world.showSection(util.select.fromTo(0, 3, 0, 4, 3, 3), Direction.DOWN);

        scene.overlay.showControls(
                new InputWindowElement(util.vector.topOf(0, 2, 1), Pointing.UP)
                        .withItem(ClockworkItems.BLUPERGLUE.asStack())
                        .rightClick(),
                40);
        scene.idle(6);

        scene.effects.indicateSuccess(util.grid.at(0, 2, 1));

        scene.idle(45);
        scene.overlay.showControls(
                new InputWindowElement(util.vector.blockSurface(util.grid.at(3, 3, 3), Direction.DOWN), Pointing.DOWN)
                        .withItem(ClockworkItems.BLUPERGLUE.asStack())
                        .rightClick(),
                40);
        scene.idle(6);

        AABB bb = new AABB(util.grid.at(0, 2, 1));
        scene.overlay.chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb, 1);
        scene.overlay.chaseBoundingBoxOutline(PonderPalette.BLUE, lever, bb.expandTowards(4, 1, 2), 285);

        //TODO add gravitron click element
        //TODO And maybe crate a ship world element??

        scene.idle(37 * 4);
    }

    private static void redstoneResistor(SceneBuilder scene, SceneBuildingUtil util){
        scene.title("resistor", "Using the Redstone Resisitor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1);
        scene.idle(15);

        Selection lever = util.select.position(2, 1, 1);
        Selection resistor = util.select.position(2, 1, 3);
        Selection redstone = util.select.position(2, 1, 2);
        Selection rot1 = util.select.fromTo(0,1,3,1,1,3);
        Selection rot2 = util.select.fromTo(3,1,3,4,1,3);

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
                .pointAt(util.vector.blockSurface(util.grid.at(4,1,3), Direction.WEST));
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
        scene.world.modifyBlock(util.grid.at(2, 1, 2), s -> s.setValue(power,  5), false);
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
                .pointAt(util.vector.blockSurface(util.grid.at(0,1,3), Direction.WEST));
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
                .pointAt(util.vector.blockSurface(util.grid.at(0,1,3), Direction.WEST));
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
                .pointAt(util.vector.blockSurface(util.grid.at(0,1,3), Direction.WEST));

        scene.idle(45);

        //scene.world.toggleRedstonePower(lever);
        //scene.world.toggleRedstonePower(redstone);
        //0,1,3   1,1,3   3,1,3   4,1,3
        scene.idle(37 * 4);
    }
}
