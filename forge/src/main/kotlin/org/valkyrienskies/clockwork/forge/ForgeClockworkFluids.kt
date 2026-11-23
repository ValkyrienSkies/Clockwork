package org.valkyrienskies.clockwork.forge

import com.tterrag.registrate.util.entry.FluidEntry
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.fluids.ForgeFlowingFluid
import org.valkyrienskies.clockwork.ClockworkFluids
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE

object ForgeClockworkFluids {
    @JvmField
    val TAR: FluidEntry<ForgeFlowingFluid.Flowing> = REGISTRATE
        .fluid("tar", ClockworkMod.asResource("fluid/tar_still"), ClockworkMod.asResource("fluid/tar_flow"))
        .lang("Tar")
        .properties { it.descriptionId("fluid.vs_clockwork.tar").viscosity(2000).density(1400) }
        .fluidProperties { it.levelDecreasePerBlock(2).tickRate(25).slopeFindDistance(3).explosionResistance(100f) }
        .source { ForgeFlowingFluid.Source(it) }
        .block()
        .properties { it.mapColor(MapColor.COLOR_BLACK) }
        .build()
        .bucket()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .onRegister(ClockworkFluids::registerBucket)
        .build()
        .register()

    @JvmStatic
    fun register() {}
}
