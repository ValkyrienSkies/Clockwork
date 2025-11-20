package org.valkyrienskies.clockwork.util.builder

import com.simibubi.create.Create
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.tterrag.registrate.util.nullness.NonNullBiConsumer
import com.tterrag.registrate.util.nullness.NonNullSupplier
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkStress
import java.util.function.Supplier

object BuilderTransformersClockwork {
    private fun <B : RotatedPillarKineticBlock, P> encasedBase(
        b: BlockBuilder<B, P>,
        drop: Supplier<ItemLike>
    ): BlockBuilder<B, P> {
        return b.initialProperties { SharedProperties.stone() }
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(ClockworkStress.setNoImpact<B, P>())
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

    fun <B : Block?, P> bearing(
        prefix: String?,
        backTexture: String?
    ): NonNullUnaryOperator<BlockBuilder<B, P>> {
        val baseBlockModelLocation = ClockworkMod.asResource("block/bearing/block")
        val baseItemModelLocation = ClockworkMod.asResource("block/bearing/item")
        val topTextureLocation = ClockworkMod.asResource("block/bearing_top")
        val sideTextureLocation = ClockworkMod.asResource("block/" + prefix + "_bearing_side")
        val backTextureLocation = ClockworkMod.asResource("block/" + backTexture)
        return NonNullUnaryOperator { b: BlockBuilder<B, P>? ->
            b!!.initialProperties(NonNullSupplier { SharedProperties.stone() })
                .properties(NonNullUnaryOperator { p: BlockBehaviour.Properties -> p.noOcclusion() })
                .blockstate(NonNullBiConsumer { c: DataGenContext<Block, B>, p: RegistrateBlockstateProvider? ->
                    p!!.directionalBlock(
                        c!!.get(), p.models()
                            .withExistingParent(c.getName(), baseBlockModelLocation)
                            .texture("side", sideTextureLocation)
                            .texture("back", backTextureLocation)
                    )
                })
                .item()
                .model(NonNullBiConsumer { c: DataGenContext<Item?, BlockItem?>?, p: RegistrateItemModelProvider? ->
                    p!!.withExistingParent(c!!.getName(), baseItemModelLocation)
                        .texture("top", topTextureLocation)
                        .texture("side", sideTextureLocation)
                        .texture("back", backTextureLocation)
                })
                .build()
        }
    }
}
