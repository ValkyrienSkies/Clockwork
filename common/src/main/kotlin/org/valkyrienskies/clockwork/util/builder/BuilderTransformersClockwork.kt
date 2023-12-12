package org.valkyrienskies.clockwork.util.builder

import com.simibubi.create.content.kinetics.BlockStressDefaults
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import org.valkyrienskies.clockwork.ClockworkMod
import java.util.function.Supplier

object BuilderTransformersClockwork {
    private fun <B : RotatedPillarKineticBlock?, P> encasedBase(
        b: BlockBuilder<B, P>,
        drop: Supplier<ItemLike>
    ): BlockBuilder<B, P> {
        return b.initialProperties { SharedProperties.stone() }
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(BlockStressDefaults.setNoImpact())
            .loot { p, lb -> p.dropOther(lb, drop.get()) }
    }

    fun <B : Block?, P> infuser(): NonNullUnaryOperator<BlockBuilder<B, P>> {
        val baseBlockModelLocation: ResourceLocation = ClockworkMod.asResource("block/physics_infuser/block")
        val baseItemModelLocation: ResourceLocation = ClockworkMod.asResource("block/physics_infuser/item")
        val liquidTextureLocation: ResourceLocation = ClockworkMod.asResource("block/physics_infuser/liquid")
        val coreTextureLocation: ResourceLocation = ClockworkMod.asResource("block/physics_infuser/core")
        return NonNullUnaryOperator { b: BlockBuilder<B, P> ->
            b.initialProperties { SharedProperties.stone() }
                .properties { p: BlockBehaviour.Properties -> p.noOcclusion() }
                .blockstate { c: DataGenContext<Block?, B>, p: RegistrateBlockstateProvider ->
                    p.directionalBlock(
                        c.get(),
                        p.models()
                            .withExistingParent(c.name, baseBlockModelLocation)
                            .texture("0", baseBlockModelLocation)
                            .texture("1", coreTextureLocation)
                            .texture("2", liquidTextureLocation)
                    )
                }
                .item()
                .model { c: DataGenContext<Item?, BlockItem?>, p: RegistrateItemModelProvider ->
                    p.withExistingParent(c.name, baseItemModelLocation)
                        .texture("0", baseBlockModelLocation)
                        .texture("1", coreTextureLocation)
                        .texture("2", liquidTextureLocation)
                }
                .build()
        }
    }

    fun <B : Block?, P> flapbearing(): NonNullUnaryOperator<BlockBuilder<B, P>> {
        val baseBlockModelLocation: ResourceLocation = ClockworkMod.asResource("block/flap_bearing/block")
        val baseItemModelLocation: ResourceLocation = ClockworkMod.asResource("block/bearing/item")
        val topTextureLocation: ResourceLocation = ClockworkMod.asResource("block/flap_bearing/top")
        val baseTextureLocation: ResourceLocation = ClockworkMod.asResource("block/flap_bearing")
        return NonNullUnaryOperator { b: BlockBuilder<B, P> ->
            b.initialProperties { SharedProperties.stone() }
                .properties { p: BlockBehaviour.Properties -> p.noOcclusion() }
                .blockstate { c: DataGenContext<Block?, B>, p: RegistrateBlockstateProvider ->
                    p.directionalBlock(
                        c.get(), p.models()
                            .withExistingParent(c.name, baseBlockModelLocation)
                            .texture("0", baseBlockModelLocation)
                            .texture("1", topTextureLocation)
                            .texture("2", baseTextureLocation)
                    )
                }
                .item()
                .model { c: DataGenContext<Item?, BlockItem?>, p: RegistrateItemModelProvider ->
                    p.withExistingParent(c.name, baseItemModelLocation)
                        .texture("0", baseBlockModelLocation)
                        .texture("1", topTextureLocation)
                        .texture("2", baseTextureLocation)
                }
                .build()
        }
    }
}