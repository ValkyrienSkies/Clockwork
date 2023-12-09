package org.valkyrienskies.clockwork;

import com.simibubi.create.foundation.ponder.*;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class ClockworkPonderScenes {
    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(ClockworkMod.MOD_ID);

    public static void init() {
        HELPER.forComponents(ClockworkItems.BLUPERGLUE)
                .addStoryBoard("bluper_glue", ClockworkPonderScenes::createShip);
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

        scene.idle(37 * 4);
    }
}
