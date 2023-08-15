package org.valkyrienskies.clockwork.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pointing;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonnullType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.valkyrienskies.clockwork.content.logistics.heat.pipe.HeatPipeBlock;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CWBlockStateGen {
    public static <P extends HeatPipeBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> pipe() {
        return (c, p) -> {
            String path = "block/" + c.getName();

            String LU = "lu";
            String RU = "ru";
            String LD = "ld";
            String RD = "rd";
            String LR = "lr";
            String UD = "ud";
            String U = "u";
            String D = "d";
            String L = "l";
            String R = "r";

            List<String> orientations = ImmutableList.of(LU, RU, LD, RD, LR, UD, U, D, L, R);
            Map<String, Pair<Integer, Integer>> uvs = ImmutableMap.<String, Pair<Integer, Integer>>builder()
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
                    .build();

            Map<Direction.Axis, ResourceLocation> coreTemplates = new IdentityHashMap<>();
            Map<Pair<String, Direction.Axis>, ModelFile> coreModels = new HashMap<>();

            for (Direction.Axis axis : Iterate.axes)
                coreTemplates.put(axis, p.modLoc(path + "/core_" + axis.getSerializedName()));

            for (Direction.Axis axis : Iterate.axes) {
                ResourceLocation parent = coreTemplates.get(axis);
                for (String s : orientations) {
                    Pair<String, Direction.Axis> key = Pair.of(s, axis);
                    String modelName = path + "/" + s + "_" + axis.getSerializedName();
                    coreModels.put(key, p.models()
                            .withExistingParent(modelName, parent)
                            .element()
                            .from(4, 4, 4)
                            .to(12, 12, 12)
                            .face(Direction.get(Direction.AxisDirection.POSITIVE, axis))
                            .end()
                            .face(Direction.get(Direction.AxisDirection.NEGATIVE, axis))
                            .end()
                            .faces((d, builder) -> {
                                Pair<Integer, Integer> pair = uvs.get(s);
                                float u = pair.getKey();
                                float v = pair.getValue();
                                if (d == Direction.UP)
                                    builder.uvs(u + 4, v + 4, u, v);
                                if (d == Direction.DOWN)
                                    builder.uvs(u + 4, v, u, v + 4);
                                if (d == Direction.NORTH)
                                    builder.uvs(u, v, u + 4, v + 4);
                                if (d == Direction.SOUTH)
                                    builder.uvs(u + 4, v, u, v + 4);
                                if (d == Direction.EAST)
                                    builder.uvs(u, v, u + 4, v + 4);
                                if (d == Direction.WEST)
                                    builder.uvs(u + 4, v, u, v + 4);
                                builder.texture("#0");
                            })
                            .end());
                }
            }

            MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
            for (Direction.Axis axis : Iterate.axes) {
                putPart(coreModels, builder, axis, LU, true, false, true, false);
                putPart(coreModels, builder, axis, RU, true, false, false, true);
                putPart(coreModels, builder, axis, LD, false, true, true, false);
                putPart(coreModels, builder, axis, RD, false, true, false, true);
                putPart(coreModels, builder, axis, UD, true, true, false, false);
                putPart(coreModels, builder, axis, U, true, false, false, false);
                putPart(coreModels, builder, axis, D, false, true, false, false);
                putPart(coreModels, builder, axis, LR, false, false, true, true);
                putPart(coreModels, builder, axis, L, false, false, true, false);
                putPart(coreModels, builder, axis, R, false, false, false, true);
            }
        };
    }

    private static void putPart(Map<Pair<String, Direction.Axis>, ModelFile> coreModels, MultiPartBlockStateBuilder builder,
                                Direction.Axis axis, String s, boolean up, boolean down, boolean left, boolean right) {
        Direction positiveAxis = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Map<Direction, BooleanProperty> propertyMap = FluidPipeBlock.PROPERTY_BY_DIRECTION;

        Direction upD = Pointing.UP.getCombinedDirection(positiveAxis);
        Direction leftD = Pointing.LEFT.getCombinedDirection(positiveAxis);
        Direction rightD = Pointing.RIGHT.getCombinedDirection(positiveAxis);
        Direction downD = Pointing.DOWN.getCombinedDirection(positiveAxis);

        if (axis == Direction.Axis.Y || axis == Direction.Axis.X) {
            leftD = leftD.getOpposite();
            rightD = rightD.getOpposite();
        }

        builder.part()
                .modelFile(coreModels.get(Pair.of(s, axis)))
                .addModel()
                .condition(propertyMap.get(upD), up)
                .condition(propertyMap.get(leftD), left)
                .condition(propertyMap.get(rightD), right)
                .condition(propertyMap.get(downD), down)
                .end();
    }

    public static Function<BlockState, ConfiguredModel[]> mapToAir(@NonnullType RegistrateBlockstateProvider p) {
        return state -> ConfiguredModel.builder()
                .modelFile(p.models()
                        .getExistingFile(p.mcLoc("block/air")))
                .build();
    }
}
