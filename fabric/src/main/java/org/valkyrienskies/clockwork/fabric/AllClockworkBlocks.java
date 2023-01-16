package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.flap.FlapBearingBlock;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.infuser.PhysicsInfuserBlock;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorBearingBlock;
import org.valkyrienskies.clockwork.fabric.content.physicalities.motion.wing.WingBlock;
import org.valkyrienskies.clockwork.fabric.util.builder.BuilderTransformersClockwork;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static org.valkyrienskies.clockwork.fabric.ClockWorkModFabric.REGISTRATE;

public class AllClockworkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkModFabric.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    //////// Propellor Bearing ////////

    public static final BlockEntry<PropellorBearingBlock> PROPELLOR_BEARING =
            REGISTRATE.block("propellor_bearing", PropellorBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformers.bearing("propellor", "gearbox", false))
                    .transform(BlockStressDefaults.setImpact(12.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    //////// Flap Bearing ////////

    public static final BlockEntry<FlapBearingBlock> FLAP_BEARING =
            REGISTRATE.block("flap_bearing", FlapBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformersClockwork.flapbearing())
                    .transform(BlockStressDefaults.setImpact(12.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    static {REGISTRATE.startSection(AllSections.CURIOSITIES);}

    public static final BlockEntry<PhysicsInfuserBlock> PHYSICS_INFUSER =
            REGISTRATE.block("physics_infuser", PhysicsInfuserBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_PURPLE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("physics_infuser", "item"))
                    .register();

    public static final BlockEntry<WingBlock> WING =
            REGISTRATE.block("wing", WingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("wing", "item"))
                    .register();

    public static void register() {}
}
