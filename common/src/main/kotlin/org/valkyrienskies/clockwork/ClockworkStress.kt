package org.valkyrienskies.clockwork

import com.simibubi.create.Create
import com.tterrag.registrate.builders.BlockBuilder
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator
import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import net.createmod.catnip.config.ConfigBase
import net.createmod.catnip.platform.CatnipServices
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraftforge.common.ForgeConfigSpec
import java.util.function.DoubleSupplier


class ClockworkStress : ConfigBase() {
    protected val capacities: MutableMap<ResourceLocation?, ForgeConfigSpec.ConfigValue<Double?>?> =
        HashMap<ResourceLocation?, ForgeConfigSpec.ConfigValue<Double?>?>()
    protected val impacts: MutableMap<ResourceLocation?, ForgeConfigSpec.ConfigValue<Double?>?> =
        HashMap<ResourceLocation?, ForgeConfigSpec.ConfigValue<Double?>?>()

    override fun registerAll(builder: ForgeConfigSpec.Builder) {
        builder.comment(".", Comments.su, Comments.impact)
            .push("impact")
        DEFAULT_IMPACTS.forEach { (id: ResourceLocation?, value: Double?) ->
            this.impacts.put(
                id, builder.define<Double?>(
                    id!!.getPath(), value
                )
            )
        }
        builder.pop()

        builder.comment(".", Comments.su, Comments.capacity)
            .push("capacity")
        DEFAULT_CAPACITIES.forEach { (id: ResourceLocation?, value: Double?) ->
            this.capacities.put(
                id, builder.define<Double?>(
                    id!!.getPath(), value
                )
            )
        }
        builder.pop()
    }

    override fun getName(): String {
        return "stressValues.v" + VERSION
    }

    fun getImpact(block: Block): DoubleSupplier? {
        val id = CatnipServices.REGISTRIES.getKeyOrThrow(block)
        val value = this.impacts.get(id)
        return if (value == null) null else DoubleSupplier { value.get()!! }
    }

    fun getCapacity(block: Block): DoubleSupplier? {
        val id = CatnipServices.REGISTRIES.getKeyOrThrow(block)
        val value = this.capacities.get(id)
        return if (value == null) null else DoubleSupplier { value.get()!! }
    }

    private object Comments {
        var su: String = "[in Stress Units]"
        var impact: String =
            "Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives."
        var capacity: String = "Configure how much stress a source can accommodate for."
    }

    companion object {
        // bump this version to reset configured values.
        private const val VERSION = 2

        // IDs need to be used since configs load before registration
        private val DEFAULT_IMPACTS: Object2DoubleMap<ResourceLocation?> = Object2DoubleOpenHashMap<ResourceLocation?>()
        private val DEFAULT_CAPACITIES: Object2DoubleMap<ResourceLocation?> =
            Object2DoubleOpenHashMap<ResourceLocation?>()

        fun <B : Block, P> setNoImpact(): NonNullUnaryOperator<BlockBuilder<B, P>> {
            return setImpact<B, P>(0.0)
        }

        fun <B : Block, P> setImpact(value: Double): NonNullUnaryOperator<BlockBuilder<B, P>> {
            return NonNullUnaryOperator { builder: BlockBuilder<B, P> ->
                assertFromClockwork(builder)
                val id = Create.asResource(builder.getName())
                DEFAULT_IMPACTS.put(id, value)
                builder
            }
        }

        fun <B : Block, P> setCapacity(value: Double): NonNullUnaryOperator<BlockBuilder<B, P>> {
            return NonNullUnaryOperator { builder: BlockBuilder<B, P> ->
                assertFromClockwork(builder)
                val id = Create.asResource(builder.getName())
                DEFAULT_CAPACITIES.put(id, value)
                builder
            }
        }

        private fun assertFromClockwork(builder: BlockBuilder<*, *>) {
            check(
                builder.getOwner().getModid() == ClockworkMod.MOD_ID
            ) { "Non-Clockwork blocks cannot be added to Clockwork's config." }
        }
    }
}
