package org.valkyrienskies.clockwork;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.*;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class ClockworkPonderScenes {

    public static final PonderTag CLOCKWORK = new PonderTag(new ResourceLocation(ClockworkMod.MOD_ID, "clockwork")).item(ClockworkItems.BLUPERGLUE.get(), true, false)
            .defaultLang("Clockwork", "How do use clockwork");

    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(ClockworkMod.MOD_ID);

    public static void init() {
        HELPER.addStoryBoard(ClockworkItems.BLUPERGLUE, "bluper_glue", ClockworkPonderScenes::createShip, AllPonderTags.CONTRAPTION_ASSEMBLY, CLOCKWORK);


        PonderRegistry.TAGS.forTag(AllPonderTags.CONTRAPTION_ASSEMBLY)
                .add(ClockworkItems.BLUPERGLUE);
    }

    private static void createShip(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("bluper_glue", "Creating Ships with Bluper Glue");
        scene.configureBasePlate(1, 1, 5);
        scene.setSceneOffsetY(-1);
        scene.world.showSection(util.select.layer(0), Direction.UP);
        scene.idle(5);
        scene.world.showSection(util.select.layer(1), Direction.DOWN);
        scene.idle(10);
        scene.world.showSection(util.select.layer(2), Direction.DOWN);
        scene.idle(10);

        scene.world.replaceBlocks(util.select.fromTo(2, 4, 3, 4, 3, 3), Blocks.OAK_PLANKS.defaultBlockState(), false);
        scene.idle(10);

        scene.overlay.showOutline(PonderPalette.BLUE, "glue", util.select.position(2, 4, 3)
                .add(util.select.fromTo(4, 3, 3, 2, 3, 3))
                .add(util.select.position(4, 3, 2)), 40);

        scene.overlay.showControls(new InputWindowElement(util.vector.centerOf(util.grid.at(4, 3, 3)), Pointing.RIGHT)
                .withItem(ClockworkItems.BLUPERGLUE.asStack()), 40);

        Selection plank2 = util.select.position(4, 3, 2);
        ElementLink<WorldSectionElement> contraption = scene.world.showIndependentSection(util.select.layersFrom(3)
                .substract(plank2), Direction.DOWN);

        scene.idle(10);
        scene.world.showSectionAndMerge(plank2, Direction.SOUTH, contraption);
        scene.idle(15);
        scene.effects.superGlue(util.grid.at(4, 3, 2), Direction.SOUTH, true);

        scene.idle(37 * 4);
    }
}
