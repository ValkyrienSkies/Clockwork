package org.valkyrienskies.clockwork;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MaterialColor;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.infuser.PhysicsInfuserBlock;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlock;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlock;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.FlapBlock;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.WingBlock;
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork;

import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    public static final BlockEntry<PropellorBearingBlock> PROPELLOR_BEARING =
            REGISTRATE.block("propellor_bearing", PropellorBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformers.bearing("propellor", "gearbox", false))
                    .transform(BlockStressDefaults.setImpact(12.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();
    public static final BlockEntry<FlapBearingBlock> FLAP_BEARING =
            REGISTRATE.block("flap_bearing", FlapBearingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.PODZOL))
                    .transform(BuilderTransformersClockwork.flapbearing())
                    .transform(BlockStressDefaults.setImpact(12.0))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .register();

    //////// Propellor Bearing ////////
    /////// Sequenced Seat ////////
    public static final BlockEntry<SequencedSeatBlock> COMMAND_SEAT =
            REGISTRATE.block("command_seat", SequencedSeatBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_LIGHT_GREEN))
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("command_seat", "item"))
                    .register();

    //////// Flap Bearing ////////
    public static final BlockEntry<PhysicsInfuserBlock> PHYSICS_INFUSER =
            REGISTRATE.block("physics_infuser", PhysicsInfuserBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.COLOR_PURPLE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("physics_infuser", "item"))
                    .register();

    static {
        REGISTRATE.startSection(AllSections.CURIOSITIES);
    }

    /////// WINX CLUB //////
    public static final BlockEntry<WingBlock> WING =
            REGISTRATE.block("wing", WingBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("wing", "item"))
                    .register();
    public static final BlockEntry<FlapBlock> FLAP =
            REGISTRATE.block("flap", FlapBlock::new)
                    .transform(axeOrPickaxe())
                    .properties(p -> p.color(MaterialColor.TERRACOTTA_WHITE))
                    .addLayer(() -> RenderType::cutoutMipped)
                    .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
                    .item()
                    .transform(ModelGen.customItemModel("wing", "item"))
                    .register();

    /////// Physics infuser ////////

    public static void register() {
    }
}
