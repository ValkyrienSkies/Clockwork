package org.valkyrienskies.clockwork

import com.simibubi.create.AllTags
import com.simibubi.create.content.kinetics.BlockStressDefaults
import com.simibubi.create.foundation.data.*
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlock
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlock
import org.valkyrienskies.clockwork.content.kinetics.resistor.BrassRedstoneResistorBlock
import org.valkyrienskies.clockwork.content.kinetics.resistor.RedstoneResistorBlock
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlock
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem
import org.valkyrienskies.clockwork.content.physicalities.wing.FlapBlock
import org.valkyrienskies.clockwork.content.physicalities.wing.WingBlock
import org.valkyrienskies.clockwork.util.builder.BuilderTransformersClockwork.flapbearing
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate
import org.valkyrienskies.clockwork.util.render.WingBlockItemRenderer
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
            .register()

    @JvmField
    val DELIVERY_CANNON: BlockEntry<DeliveryCannonBlock> =
        REGISTRATE.block<DeliveryCannonBlock>("delivery_cannon") { properties: BlockBehaviour.Properties? ->
            DeliveryCannonBlock(properties!!)
        }
            .initialProperties { SharedProperties.netheriteMetal() }
            .properties {
                it.sound(SoundType.METAL)
            }
            .transform(BlockStressDefaults.setImpact(4.0))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .register()

    @JvmField
    val DELIVERY_CHUTE: BlockEntry<DeliveryChuteBlock> =
        REGISTRATE.block<DeliveryChuteBlock>("delivery_chute") { properties: BlockBehaviour.Properties? ->
            DeliveryChuteBlock(properties!!)
        }
            .initialProperties { SharedProperties.netheriteMetal() }
            .properties {
                it.sound(SoundType.METAL)
            }
            .transform(BlockStressDefaults.setImpact(4.0))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .register()

    @JvmField
    val ALT_METER: BlockEntry<AltMeterBlock> =
        REGISTRATE.block<AltMeterBlock>("alt_meter") { properties: BlockBehaviour.Properties? ->
            AltMeterBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .transform(TagGen.axeOrPickaxe())
            .addLayer { Supplier { RenderType.cutout() } }
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
            .model(AssetLookup.customBlockItemModel("alt_meter"))
            .build()
            .register()

    @JvmField
    val GYRO: BlockEntry<GyroBlock> = REGISTRATE.block<GyroBlock>("gyro") { properties: BlockBehaviour.Properties? ->
        GyroBlock(properties!!)
    }
        .initialProperties { SharedProperties.stone() }
        .properties {
            it.noOcclusion().mapColor(MapColor.TERRACOTTA_YELLOW)
        }
        .transform(TagGen.axeOrPickaxe())
        .addLayer { Supplier { RenderType.cutout() } }
        .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
        .item()
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
            .transform(
                ModelGen.customItemModel<BlockItem, BlockBuilder<RedstoneResistorBlock, CreateRegistrate>>(
                    "redstone_resistor",
                    "item"
                )
            )
            .register()

    @JvmField
    val BRASS_REDSTONE_RESISTOR: BlockEntry<BrassRedstoneResistorBlock> =
        REGISTRATE.block<BrassRedstoneResistorBlock>("brass_redstone_resistor") { properties: BlockBehaviour.Properties? ->
            BrassRedstoneResistorBlock(properties!!)
        }
            .initialProperties { SharedProperties.stone() }
            .properties {
                it.noOcclusion()
            }
            .transform<Block, BrassRedstoneResistorBlock, CreateRegistrate, BlockBuilder<BrassRedstoneResistorBlock, CreateRegistrate>>(
                BlockStressDefaults.setNoImpact()
            )
            .transform<Block, BrassRedstoneResistorBlock, CreateRegistrate, BlockBuilder<BrassRedstoneResistorBlock, CreateRegistrate>>(
                TagGen.axeOrPickaxe()
            )
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .item()
            .transform(
                ModelGen.customItemModel<BlockItem, BlockBuilder<BrassRedstoneResistorBlock, CreateRegistrate>>(
                    "brass_redstone_resistor",
                    "item"
                )
            )
            .register()

    @JvmField
    val COMMAND_SEAT: BlockEntry<SequencedSeatBlock> =
        REGISTRATE.block<SequencedSeatBlock>("command_seat") { properties: BlockBehaviour.Properties? ->
            SequencedSeatBlock(properties!!)
        }
            .transform(TagGen.axeOrPickaxe())
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
            .item()
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
        .transform(ClockworkRegistrate.customRenderedBlockItem { Supplier { WingBlockItemRenderer() } })
        .register()


    @JvmStatic
    fun register() {
    }
}