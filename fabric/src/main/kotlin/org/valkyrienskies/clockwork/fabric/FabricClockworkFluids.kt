package org.valkyrienskies.clockwork.fabric

import com.tterrag.registrate.fabric.SimpleFlowableFluid
import com.tterrag.registrate.util.entry.FluidEntry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.MapColor
import org.valkyrienskies.clockwork.ClockworkFluids
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE

object FabricClockworkFluids {
    @JvmField
    val TAR: FluidEntry<SimpleFlowableFluid.Flowing> = REGISTRATE
        .fluid("tar", ClockworkMod.asResource("fluid/tar_still"), ClockworkMod.asResource("fluid/tar_flow"))
        .lang("Tar")
        .fluidProperties { it.levelDecreasePerBlock(2).tickRate(25).flowSpeed(3).blastResistance(100f) }
        .fluidAttributes {
            object : FluidVariantAttributeHandler {
                override fun getName(variant: FluidVariant): Component = Component.translatable("fluid.vs_clockwork.tar")

                override fun getViscosity(variant: FluidVariant, level: Level?): Int = 2000
            }
        }
        .source { SimpleFlowableFluid.Source(it) }
        .block()
        .properties { it.mapColor(MapColor.COLOR_BLACK) }
        .build()
        .bucket()
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .onRegister(ClockworkFluids::registerTarBucket)
        .build()
        .onRegisterAfter(Registries.ITEM) { flowing ->
            val source = flowing.source
            FluidStorage.combinedItemApiProvider(source.bucket).register { context ->
                FullItemFluidStorage(context, { ItemVariant.of(Items.BUCKET) }, FluidVariant.of(source), FluidConstants.BUCKET)
            }
            FluidStorage.combinedItemApiProvider(Items.BUCKET).register { context ->
                EmptyItemFluidStorage(context, { ItemVariant.of(source.bucket) }, source, FluidConstants.BUCKET)
            }
        }
        .register()

    @JvmStatic
    fun register() {}
}
