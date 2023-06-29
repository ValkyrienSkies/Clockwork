package org.valkyrienskies.clockwork.ponder.scene;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.Vec3;

public class CombustionEngineScene {
    public static void explanationScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("combustion_engine_essential", 
                new TranslatableComponent("vs_clockwork.ponder.combustion_engine.essential.header")
                        .getString());
        scene.showBasePlate();

        scene.world.showSection(util.select.position(2, 1, 2), Direction.DOWN);
        scene.overlay
                .showText(40)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.essential.engine")
                        .getString())
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.idle(50);
        scene.addKeyframe();

        scene.world.showIndependentSection(util.select.position(1, 1, 2), Direction.EAST);
        scene.world.showIndependentSection(util.select.position(3, 1, 2), Direction.WEST);
        scene.overlay
                .showText(30)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.essential.output")
                        .getString())
                //.pointAt(util.vector.of(4.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.addKeyframe();
        scene.idle(40);

        scene.world.showIndependentSection(util.select.position(2, 2, 2), Direction.DOWN);
        scene.overlay
                .showText(30)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.essential.input")
                        .getString())
                //.pointAt(util.vector.of(3.5, 2.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.addKeyframe();
        scene.idle(40);
    }

    public static void frostingEffectExplanation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("combustion_engine_frosting", 
                new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.header")
                        .getString());
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        Selection vanilla = util.select.fromTo(0, 1, 0, 4, 2, 0);
        Selection chocolate = util.select.fromTo(0, 1, 2, 4, 2, 2);
        Selection strawberry = util.select.fromTo(0, 1, 4, 4, 2, 4);

        Selection initialGearAssembly = util.select.position(5, 0, 0)
                .add(util.select.fromTo(3, 1, 1, 5, 1, 1));
        Selection connectGear = util.select.position(3, 1, 3);

        scene.world.showIndependentSection(initialGearAssembly, Direction.EAST);
        scene.world.showSection(vanilla, Direction.DOWN);
        scene.overlay
                .showText(60)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.vanilla.explanation")
                        .getString())
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.overlay
                .showText(200)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.vanilla.output")
                        .getString())
                .pointAt(new Vec3(1, 1.3, 0.5))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);
        scene.addKeyframe();

        scene.world.showSection(chocolate, Direction.DOWN);
        scene.overlay
                .showText(60)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.chocolate.explanation")
                        .getString())
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.overlay
                .showText(130)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.chocolate.output")
                        .getString())
                .pointAt(new Vec3(1, 1.3, 2.5))
                .placeNearTarget()
                .colored(PonderPalette.BLUE);
        scene.idle(70);
        scene.addKeyframe();

        scene.world.showIndependentSection(connectGear, Direction.DOWN);
        scene.world.showSection(strawberry, Direction.DOWN);
        scene.overlay
                .showText(60)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.strawberry.explanation")
                        .getString())
                //.pointAt(util.vector.of(2.5, 1.3, 2.5))
                .colored(PonderPalette.RED)
                .placeNearTarget();
        scene.overlay
                .showText(60)
                .text(new TranslatableComponent("vs_clockwork.ponder.combustion_engine.frosting.strawberry.output")
                        .getString())
                .pointAt(new Vec3(1, 1.3, 4.5))
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
