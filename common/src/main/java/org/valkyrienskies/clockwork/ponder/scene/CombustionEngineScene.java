package org.valkyrienskies.clockwork.ponder.scene;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class CombustionEngineScene {
    public static void explanationScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("combustion_engine_essential", "Use");
        scene.showBasePlate();

        scene.world.showSection(util.select.position(2, 1, 2), Direction.DOWN);
        scene.overlay
                .showText(40)
                .text("")
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.idle(50);
        scene.addKeyframe();

        scene.world.showIndependentSection(util.select.position(1, 1, 2), Direction.EAST);
        scene.world.showIndependentSection(util.select.position(3, 1, 2), Direction.WEST);
        scene.overlay
                .showText(30)
                .text("")
                //.pointAt(util.vector.of(4.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.addKeyframe();
        scene.idle(40);

        scene.world.showIndependentSection(util.select.position(2, 2, 2), Direction.DOWN);
        scene.overlay
                .showText(30)
                .text("")
                //.pointAt(util.vector.of(3.5, 2.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.addKeyframe();
        scene.idle(40);
    }

    public static void frostingEffectExplanation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("combustion_engine_frosting", "Frosting");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        scene.world.showSection(util.select.fromTo(1, 1, 0, 6, 2, 0)
                .add(util.select.fromTo(0, 1, 3, 0, 1, 5))
                .add(util.select.position(6, 0, 1)), Direction.DOWN);
        scene.overlay
                .showText(60)
                .text("vanilla explanation")
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.overlay
                .showText(200)
                .text("RPM and SU provided")
                .pointAt(new Vec3(1, 1.3, 1.5))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);
        scene.addKeyframe();

        scene.world.showSection(util.select.fromTo(0, 1, 1, 5, 2, 2), Direction.DOWN);
        scene.overlay
                .showText(60)
                .text("chocolate explanation")
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.overlay
                .showText(130)
                .text("RPM and SU provided")
                .pointAt(new Vec3(1, 1.3, 3.5))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);
        scene.addKeyframe();

        scene.world.showSection(util.select.fromTo(0, 1, 3, 5, 2, 4), Direction.DOWN);
        scene.overlay
                .showText(60)
                .text("strawberry explanation")
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.overlay
                .showText(60)
                .text("RPM and SU provided")
                .pointAt(new Vec3(1, 1.3, 5.5))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);
        scene.addKeyframe();
    }

    public static void selfSufficientExplanation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("combustion_engine_self", "Use");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
    }
}
