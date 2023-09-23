package org.valkyrienskies.clockwork.data

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock
import com.simibubi.create.foundation.utility.Iterate
import com.simibubi.create.foundation.utility.Pointing
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.nullness.NonNullBiConsumer
import com.tterrag.registrate.util.nullness.NonnullType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.generators.BlockModelBuilder
import net.minecraftforge.client.model.generators.ConfiguredModel
import net.minecraftforge.client.model.generators.ModelBuilder
import net.minecraftforge.client.model.generators.ModelFile
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder
import org.apache.commons.lang3.tuple.Pair
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlock
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function

object CWBlockStateGen {
    fun <P : HeatPipeBlock?> pipe(): NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> {
        return NonNullBiConsumer { c: DataGenContext<Block, P>, p: RegistrateBlockstateProvider ->
            val path = "block/" + c.name
            val LU = "lu"
            val RU = "ru"
            val LD = "ld"
            val RD = "rd"
            val LR = "lr"
            val UD = "ud"
            val U = "u"
            val D = "d"
            val L = "l"
            val R = "r"
            val orientations: List<String> =
                ImmutableList.of(LU, RU, LD, RD, LR, UD, U, D, L, R)
            val uvs: Map<String, Pair<Int, Int>> =
                ImmutableMap.builder<String, Pair<Int, Int>>()
                    .put(LU, Pair.of(12, 4))
                    .put(RU, Pair.of(8, 4))
                    .put(LD, Pair.of(12, 0))
                    .put(RD, Pair.of(8, 0))
                    .put(LR, Pair.of(4, 8))
                    .put(UD, Pair.of(0, 8))
                    .put(U, Pair.of(4, 4))
                    .put(D, Pair.of(0, 0))
                    .put(L, Pair.of(4, 0))
                    .put(R, Pair.of(0, 4))
                    .build()
            val coreTemplates: MutableMap<Direction.Axis, ResourceLocation> =
                IdentityHashMap()
            val coreModels: MutableMap<Pair<String, Direction.Axis>, ModelFile> =
                HashMap()
            for (axis in Iterate.axes) coreTemplates[axis] =
                p.modLoc(path + "/core_" + axis.serializedName)
            for (axis in Iterate.axes) {
                val parent = coreTemplates[axis]
                for (s in orientations) {
                    val key =
                        Pair.of(
                            s,
                            axis
                        )
                    val modelName = path + "/" + s + "_" + axis.serializedName
                    coreModels[key] = p.models()
                        .withExistingParent(modelName, parent)
                        .element()
                        .from(4f, 4f, 4f)
                        .to(12f, 12f, 12f)
                        .face(
                            Direction.get(
                                Direction.AxisDirection.POSITIVE,
                                axis
                            )
                        )
                        .end()
                        .face(
                            Direction.get(
                                Direction.AxisDirection.NEGATIVE,
                                axis
                            )
                        )
                        .end()
                        .faces(BiConsumer { d: Direction, builder: ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder ->
                            val (key1, value) = uvs[s]!!
                            val u = key1.toFloat()
                            val v = value.toFloat()
                            if (d == Direction.UP) builder.uvs(u + 4, v + 4, u, v)
                            if (d == Direction.DOWN) builder.uvs(u + 4, v, u, v + 4)
                            if (d == Direction.NORTH) builder.uvs(u, v, u + 4, v + 4)
                            if (d == Direction.SOUTH) builder.uvs(u + 4, v, u, v + 4)
                            if (d == Direction.EAST) builder.uvs(u, v, u + 4, v + 4)
                            if (d == Direction.WEST) builder.uvs(u + 4, v, u, v + 4)
                            builder.texture("#0")
                        })
                        .end()
                }
            }
            val builder =
                p.getMultipartBuilder(c.get())
            for (axis in Iterate.axes) {
                putPart(coreModels, builder, axis, LU, true, false, true, false)
                putPart(coreModels, builder, axis, RU, true, false, false, true)
                putPart(coreModels, builder, axis, LD, false, true, true, false)
                putPart(coreModels, builder, axis, RD, false, true, false, true)
                putPart(coreModels, builder, axis, UD, true, true, false, false)
                putPart(coreModels, builder, axis, U, true, false, false, false)
                putPart(coreModels, builder, axis, D, false, true, false, false)
                putPart(coreModels, builder, axis, LR, false, false, true, true)
                putPart(coreModels, builder, axis, L, false, false, true, false)
                putPart(coreModels, builder, axis, R, false, false, false, true)
            }
        }
    }

    private fun putPart(
        coreModels: Map<Pair<String, Direction.Axis>, ModelFile>, builder: MultiPartBlockStateBuilder,
        axis: Direction.Axis, s: String, up: Boolean, down: Boolean, left: Boolean, right: Boolean
    ) {
        val positiveAxis = Direction.get(Direction.AxisDirection.POSITIVE, axis)
        val propertyMap = FluidPipeBlock.PROPERTY_BY_DIRECTION
        val upD = Pointing.UP.getCombinedDirection(positiveAxis)
        var leftD = Pointing.LEFT.getCombinedDirection(positiveAxis)
        var rightD = Pointing.RIGHT.getCombinedDirection(positiveAxis)
        val downD = Pointing.DOWN.getCombinedDirection(positiveAxis)
        if (axis === Direction.Axis.Y || axis === Direction.Axis.X) {
            leftD = leftD.opposite
            rightD = rightD.opposite
        }
        builder.part()
            .modelFile(coreModels[Pair.of(s, axis)])
            .addModel()
            .condition(propertyMap[upD], up)
            .condition(propertyMap[leftD], left)
            .condition(propertyMap[rightD], right)
            .condition(propertyMap[downD], down)
            .end()
    }

    fun mapToAir(p: @NonnullType RegistrateBlockstateProvider): Function<BlockState, Array<ConfiguredModel>> {
        return Function { state: BlockState ->
            ConfiguredModel.builder()
                .modelFile(
                    p.models()
                        .getExistingFile(p.mcLoc("block/air"))
                )
                .build()
        }
    }
}