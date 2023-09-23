package org.valkyrienskies.clockwork.util.builder

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.decoration.encasing.CasingConnectivity
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour
import com.simibubi.create.content.kinetics.BlockStressDefaults
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour
import com.simibubi.create.foundation.data.AssetLookup
import com.simibubi.create.foundation.data.BlockStateGen
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.data.SharedProperties
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.casing.ExtendedEncasedShaftBlock
import java.util.function.BiConsumer
import java.util.function.BiPredicate
import java.util.function.Function
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
                        c.get(), p.models()
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

    fun <B : ExtendedEncasedShaftBlock, P> encasedShaft(
        casing: String,
        casingShift: Supplier<CTSpriteShiftEntry?>
    ): NonNullUnaryOperator<BlockBuilder<B, P>> {
        return NonNullUnaryOperator<BlockBuilder<B, P>> { builder: BlockBuilder<B, P> ->
            encasedBase(builder,
                Supplier<ItemLike> { AllBlocks.SHAFT.get() })
                .onRegister(CreateRegistrate.connectedTextures(Supplier<ConnectedTextureBehaviour> {
                    EncasedCTBehaviour(
                        casingShift.get()
                    )
                }))
                .onRegister(CreateRegistrate.casingConnectivity(BiConsumer<B, CasingConnectivity> { block: B, cc: CasingConnectivity ->
                    cc.make(block, casingShift.get(),
                        BiPredicate<BlockState, Direction> { s: BlockState, f: Direction ->
                            f.axis !== s.getValue(
                                BlockStateProperties.AXIS
                            )
                        })
                }))
                .blockstate { c: DataGenContext<Block?, B>?, p: RegistrateBlockstateProvider ->
                    BlockStateGen.axisBlock(
                        c, p,
                        Function { blockState: BlockState? ->
                            p.models()
                                .getExistingFile(p.modLoc("block/encased_shaft/block_$casing"))
                        }, true
                    )
                }
                .item()
                .model(
                    AssetLookup.customBlockItemModel<BlockItem>(
                        "encased_shaft",
                        "item_$casing"
                    )
                )
                .build()
        }
    }
}