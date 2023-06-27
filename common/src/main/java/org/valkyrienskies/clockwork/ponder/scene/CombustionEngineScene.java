package org.valkyrienskies.clockwork.ponder.scene;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public class CombustionEngineScene {
    public static void use(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("combustion_engine", "Use");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        scene.world.showSection(util.select.position(2, 3, 2), Direction.DOWN);
        scene.overlay
                .showText(75)
                .text("The Combustion Engine is a block added as a way to provide a power source with high stress capacity")
                .pointAt( new Vec3(2.5, 3.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.idle(85);
        scene.addKeyframe();

        scene.world.setKineticSpeed(util.select.position(1, 3, 2), 16);
        scene.world.showIndependentSection(util.select.fromTo(4, 2, 2, 4, 5, 2), Direction.WEST);
        scene.world.showIndependentSection(util.select.fromTo(3, 3, 2, 3, 2, 2), Direction.WEST);
        scene.overlay
                .showText(70)
                .text("The Combustion Engine accepts Vanilla, Chocolate, or Strawberry frosting")
                .pointAt( new Vec3(3.5, 4.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.addKeyframe();
        scene.idle(80);
    }
}
