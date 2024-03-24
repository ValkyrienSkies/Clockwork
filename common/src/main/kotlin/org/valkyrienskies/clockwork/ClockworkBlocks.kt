package org.valkyrienskies.clockwork

import com.simibubi.create.AllTags
import com.simibubi.create.content.fluids.PipeAttachmentModel
import com.simibubi.create.content.kinetics.BlockStressDefaults
import com.simibubi.create.foundation.data.*
import com.simibubi.create.foundation.data.ModelGen.customItemModel
import com.simibubi.create.foundation.data.TagGen.axeOrPickaxe
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.client.render.WingBlockItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.WanderliteOreBlock
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.GooBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlock
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlock
import org.valkyrienskies.clockwork.content.logistics.heat.creative.gas.CreativeGasSourceBlock
import org.valkyrienskies.clockwork.content.logistics.heat.creative.source.CreativeHeatSourceBlock
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlock
import org.valkyrienskies.clockwork.content.logistics.heat.usage.gas_nozzle.GasNozzleBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlock
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem
import org.valkyrienskies.clockwork.content.physicalities.wing.FlapBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.WingBlock
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork.flapbearing
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate
import java.util.function.Supplier


object ClockworkBlocks {

    @JvmField
    val PROPELLER_BEARING: BlockEntry<PropellerBearingBlock> =
        REGISTRATE.block<PropellerBearingBlock>("propeller_bearing") { properties: BlockBehaviour.Properties? ->
            PropellerBearingBlock(properties!!)
        }
            .transform(TagGen.axeOrPickaxe())
            .transform(BuilderTransformers.bearing("propeller", "gearbox"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .build()
            .register()

    @JvmField
    val PHYS_BEARING: BlockEntry<PhysBearingBlock> =
        REGISTRATE.block<PhysBearingBlock>("phys_bearing") { properties: BlockBehaviour.Properties? ->
            PhysBearingBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(TagGen.axeOrPickaxe())
            .properties {
                it.lightLevel { state: BlockState? -> PhysBearingBlock.getLight(state) }
            }
            .addLayer { Supplier { RenderType.cutout() } }
            .blockstate { c: DataGenContext<Block?, PhysBearingBlock>, p: RegistrateBlockstateProvider ->
                p.directionalBlock(c.entry, AssetLookup.partialBaseModel(c, p))
            }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .model(AssetLookup.customBlockItemModel("phys_bearing"))
            .build()
            .register()

    @JvmField
    val FLAP_BEARING: BlockEntry<FlapBearingBlock> =
        REGISTRATE.block<FlapBearingBlock>("flap_bearing") { properties: BlockBehaviour.Properties? ->
            FlapBearingBlock(properties)
        }
            .transform(TagGen.axeOrPickaxe())
            .transform(flapbearing())
            .transform(BlockStressDefaults.setImpact(4.0))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .build()
            .register()



    @JvmField
    val ALT_METER: BlockEntry<AltMeterBlock> =
        REGISTRATE.block<AltMeterBlock>("alt_meter") { properties: BlockBehaviour.Properties? ->
            AltMeterBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(TagGen.axeOrPickaxe())
            .properties { it.noOcclusion() }
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .model(AssetLookup.customBlockItemModel("alt_meter"))
            .build()
            .register()

    @JvmField
    val GYRO: BlockEntry<GyroBlock> = REGISTRATE.block<GyroBlock>("gyro") { properties: BlockBehaviour.Properties? ->
        GyroBlock(properties!!)
    }
        .initialProperties { SharedProperties.stone() }
        .properties {
            it.noOcclusion()
        }
        .transform(TagGen.axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .model(AssetLookup.customBlockItemModel("gyro"))
        .build()
        .register()

    @JvmField
    val REDSTONE_RESISTOR: BlockEntry<RedstoneResistorBlock> =
        REGISTRATE.block<RedstoneResistorBlock>("redstone_resistor") { properties: BlockBehaviour.Properties? ->
            RedstoneResistorBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .properties {
                it.noOcclusion()
            }
            .transform<Block, RedstoneResistorBlock, CreateRegistrate, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                BlockStressDefaults.setNoImpact()
            )
            .transform<Block, RedstoneResistorBlock, CreateRegistrate, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                TagGen.axeOrPickaxe()
            )
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(
                ModelGen.customItemModel<BlockItem, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                    "redstone_resistor",
                    "item"
                )
            )
            .register()

    @JvmField
    val COMMAND_SEAT: BlockEntry<SequencedSeatBlock> =
        REGISTRATE.block<SequencedSeatBlock>("command_seat") { properties: BlockBehaviour.Properties? ->
            SequencedSeatBlock(properties!!)
        }
            .properties {
                it.noOcclusion()
            }
            .transform(TagGen.axeOrPickaxe())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(ModelGen.customItemModel("command_seat", "item"))
            .register()

    @JvmField
    val WING: BlockEntry<WingBlock> = REGISTRATE.block<WingBlock>("wing") { properties: BlockBehaviour.Properties? ->
        WingBlock(properties)
    }
        .transform<Block, WingBlock, CreateRegistrate, BlockBuilder<WingBlock, CreateRegistrate>>(TagGen.axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
        .item { block: WingBlock?, properties: Item.Properties? ->
            DyedWingBlockItem(block, properties)
        }
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .transform(ClockworkRegistrate.customRenderedBlockItem<DyedWingBlockItem, BlockBuilder<WingBlock, CreateRegistrate>> { Supplier { WingBlockItemRenderer() } })
        .register()

    @JvmField
    val FLAP: BlockEntry<FlapBlock> = REGISTRATE.block<FlapBlock>("flap") { properties: BlockBehaviour.Properties? ->
        FlapBlock(properties)
    }
        .transform(TagGen.axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutoutMipped() } }
        .tag(AllTags.AllBlockTags.FAN_TRANSPARENT.tag)
        .item { block: FlapBlock?, properties: Item.Properties? ->
            DyedWingBlockItem(
                block,
                properties
            )
        }
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .transform(ClockworkRegistrate.customRenderedBlockItem { Supplier { WingBlockItemRenderer() } })
        .register()

    @JvmField
    val PHYSICS_INFUSER: BlockEntry<PhysicsInfuserBlock> =
        REGISTRATE.block<PhysicsInfuserBlock>("physics_infuser") { properties: BlockBehaviour.Properties? ->
            PhysicsInfuserBlock(properties!!)
        }
            .transform<Block, PhysicsInfuserBlock, CreateRegistrate, BlockBuilder<PhysicsInfuserBlock, CreateRegistrate>>(
                axeOrPickaxe()
            )

            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(customItemModel("physics_infuser", "item"))
            .register()

    @JvmField
    val GOO_BLOCK = REGISTRATE.block<GooBlock>("goo_block") { properties: BlockBehaviour.Properties? ->
        GooBlock(
            properties!!
        )
    }
        .initialProperties { Blocks.HONEY_BLOCK }
        .addLayer { Supplier { RenderType.cutout() } }
        .item()
        //TODO .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()

    @JvmField
    val SLICKER = REGISTRATE.block<SlickerBlock>(
        "slicker"
    ) { properties: BlockBehaviour.Properties? ->
        SlickerBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.softMetal() }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .transform(customItemModel())
        .build()
        .register()

    @JvmField
    val WANDERLITE_DEEPSLATE_ORE = REGISTRATE.block<WanderliteOreBlock>(
        "wanderlite_deepslate_ore"
    ) { properties: BlockBehaviour.Properties? ->
        WanderliteOreBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()

    @JvmField
    val WANDERLITE_END_ORE = REGISTRATE.block<WanderliteOreBlock>(
        "wanderlite_end_ore"
    ) { properties: BlockBehaviour.Properties? ->
        WanderliteOreBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()

    @JvmField
    val GAS_NOZZLE = REGISTRATE.block<GasNozzleBlock>(
        "gas_nozzle"
    ) { properties: BlockBehaviour.Properties? ->
        GasNozzleBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()

    @JvmField
    val CREATIVE_GAS_SOURCE = REGISTRATE.block<CreativeGasSourceBlock>(
        "creative_gas_source"
    ) { properties: BlockBehaviour.Properties? ->
        CreativeGasSourceBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()

    @JvmField
    val CREATIVE_HEAT_SOURCE = REGISTRATE.block<CreativeHeatSourceBlock>(
        "creative_heat_source"
    ) { properties: BlockBehaviour.Properties? ->
        CreativeHeatSourceBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.netheriteMetal() }
        .addLayer { Supplier { RenderType.cutout() } }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()
    
    @JvmField
    val BALLAST = REGISTRATE.block<BallastBlock>(
        "ballast"
    ) { properties: BlockBehaviour.Properties? ->
        BallastBlock(
            properties!!
        )
    }
        .initialProperties { SharedProperties.wooden() }
        .item()
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .build()
        .register()

    @JvmStatic
    fun register() {

    }
}