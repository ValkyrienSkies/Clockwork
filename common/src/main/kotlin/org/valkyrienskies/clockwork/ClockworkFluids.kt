package org.valkyrienskies.clockwork

import dev.architectury.core.block.ArchitecturyLiquidBlock
import dev.architectury.core.fluid.ArchitecturyFlowingFluid.Flowing
import dev.architectury.core.fluid.ArchitecturyFlowingFluid.Source
import dev.architectury.core.fluid.ArchitecturyFluidAttributes
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes
import dev.architectury.core.item.ArchitecturyBucketItem
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.Fluid

object ClockworkFluids {
    private val FLUIDS: DeferredRegister<Fluid> = DeferredRegister.create(ClockworkMod.MOD_ID, Registry.FLUID_REGISTRY)
    private val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ClockworkMod.MOD_ID, Registry.BLOCK_REGISTRY)
    private val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ClockworkMod.MOD_ID, Registry.ITEM_REGISTRY)


    // FROSTING REGISTRATION
    val FROSTING_FLUID_ATTRIBUTES: ArchitecturyFluidAttributes = SimpleArchitecturyFluidAttributes.ofSupplier(
            { FROSTING_FLUID_FLOWING }) { FROSTING_FLUID }
            .blockSupplier { FROSTING_FLUID_BLOCK }
            .bucketItemSupplier { FROSTING_BUCKET }

    val FROSTING_FLUID: RegistrySupplier<Fluid> = FLUIDS.register("frosting") { Source(FROSTING_FLUID_ATTRIBUTES) }

    val FROSTING_FLUID_FLOWING: RegistrySupplier<FlowingFluid> = FLUIDS.register("flowing_frosting") { Flowing(FROSTING_FLUID_ATTRIBUTES) }

    val FROSTING_FLUID_BLOCK: RegistrySupplier<LiquidBlock> = BLOCKS.register("frosting") {
        ArchitecturyLiquidBlock(FROSTING_FLUID_FLOWING,
                BlockBehaviour.Properties.copy(Blocks.WATER))
    }

    val FROSTING_BUCKET: RegistrySupplier<Item> = ITEMS.register("frosting_bucket") { ArchitecturyBucketItem(FROSTING_FLUID, Item.Properties().tab(ClockworkMod.BASE_CREATIVE_TAB)) }


    // BUTTER REGISTRATION
    val BUTTER_FLUID_ATTRIBUTES: ArchitecturyFluidAttributes = SimpleArchitecturyFluidAttributes.ofSupplier(
            { BUTTER_FLUID_FLOWING }) { BUTTER_FLUID }
            .blockSupplier { BUTTER_FLUID_BLOCK }
            .bucketItemSupplier { BUTTER_BUCKET }

    val BUTTER_FLUID: RegistrySupplier<Fluid> = FLUIDS.register("butter") { Source(BUTTER_FLUID_ATTRIBUTES) }

    val BUTTER_FLUID_FLOWING: RegistrySupplier<FlowingFluid> = FLUIDS.register("flowing_butter") { Flowing(BUTTER_FLUID_ATTRIBUTES) }

    val BUTTER_FLUID_BLOCK: RegistrySupplier<LiquidBlock> = BLOCKS.register("butter") {
        ArchitecturyLiquidBlock(BUTTER_FLUID_FLOWING,
                BlockBehaviour.Properties.copy(Blocks.WATER))
    }

    val BUTTER_BUCKET: RegistrySupplier<Item> = ITEMS.register("butter_bucket") {
        ArchitecturyBucketItem(BUTTER_FLUID, Item.Properties().tab(ClockworkMod.BASE_CREATIVE_TAB))
    }

    fun register() {
        FLUIDS.register()
        BLOCKS.register()
        ITEMS.register()
    }
}