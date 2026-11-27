package org.valkyrienskies.clockwork.forge

import com.simibubi.create.AllFluids
import com.tterrag.registrate.util.entry.FluidEntry
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.ForgeFlowingFluid
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkFluids
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE

object ForgeClockworkFluids {
    @JvmField
    val TAR: FluidEntry<ForgeFlowingFluid.Flowing> = REGISTRATE
        .fluid("tar", ClockworkMod.asResource("fluid/tar_still"), ClockworkMod.asResource("fluid/tar_flow")) { properties, still, flowing ->
            object : AllFluids.TintedFluidType(properties, still, flowing) {
                override fun getTintColor(stack: FluidStack): Int = NO_TINT

                override fun getTintColor(state: FluidState, getter: BlockAndTintGetter, pos: BlockPos): Int = NO_TINT

                override fun getCustomFogColor(): Vector3f = Vector3f(0f)

                override fun getFogDistanceModifier(): Float = 1f / 8f
            }
        }
        .lang("Tar")
        .properties { it.descriptionId("fluid.vs_clockwork.tar").viscosity(2000).density(1400) }
        .fluidProperties { it.levelDecreasePerBlock(2).tickRate(25).slopeFindDistance(3).explosionResistance(100f) }
        .source { ForgeFlowingFluid.Source(it) }
        .block()
        .properties { it.mapColor(MapColor.COLOR_BLACK) }
        .build()
        .bucket()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .onRegister(ClockworkFluids::registerTarBucket)
        .build()
        .register()

    @JvmStatic
    fun register() {}
}
