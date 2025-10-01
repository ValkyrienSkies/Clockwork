package org.valkyrienskies.clockwork.util.builder;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItems;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.platform.SharedValues;

import java.util.function.Supplier;

public class ClockworkRegistrate {

    public static <T extends Item, P> NonNullUnaryOperator<ItemBuilder<T, P>> customRenderedItem(
            Supplier<Supplier<CustomRenderedItemModelRenderer>> supplier) {
        return b -> {
            onClient(() -> () -> customRenderedItem(b, supplier));
            return b;
        };
    }

    public static <T extends BlockItem, P> NonNullFunction<ItemBuilder<T, P>, P> customRenderedBlockItem(
            Supplier<Supplier<CustomRenderedItemModelRenderer>> supplier) {
        return b -> {
            onClient(() -> () -> customRenderedBlockItem(b, supplier));
            return b.build();
        };
    }

    public static <T extends Block> NonNullConsumer<? super T> blockModel(
            Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        return entry -> onClient(() -> () -> registerBlockModel(entry, func));
    }

    @Environment(EnvType.CLIENT)
    private static void registerBlockModel(Block entry,
                                           Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(RegisteredObjects.getKeyOrThrow(entry), func.get());
    }

    protected static void onClient(Supplier<Runnable> toRun) {
        PlatformUtils.getEnvExecutor(toRun);
    }

    @Environment(EnvType.CLIENT)
    private static <T extends Item, P> void customRenderedItem(ItemBuilder<T, P> b, Supplier<Supplier<CustomRenderedItemModelRenderer>> supplier) {
        b.onRegister(new CustomRendererRegistrationHelper(supplier));
    }

    @Environment(EnvType.CLIENT)
    private static <T extends BlockItem, P> void customRenderedBlockItem(ItemBuilder<T, P> b,
                                                                         Supplier<Supplier<CustomRenderedItemModelRenderer>> supplier) {
        b.onRegister(new CustomBlockItemRendererRegistrationHelper(supplier));
    }

    @Environment(EnvType.CLIENT)
    private static void registerItemModel(Item entry,
                                          Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomItemModels()
                .register(RegisteredObjects.getKeyOrThrow(entry), func.get());
    }

    @Environment(EnvType.CLIENT)
    private static void registerCustomRenderedBlockItem(BlockItem entry, Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomItemModels()
                .register(RegisteredObjects.getKeyOrThrow(entry), func.get());
    }

    @Environment(EnvType.CLIENT)
    private record CustomRendererRegistrationHelper(Supplier<Supplier<CustomRenderedItemModelRenderer>> supplier) implements NonNullConsumer<Item> {
        @Override
        public void accept(Item entry) {
            CustomRenderedItemModelRenderer renderer = supplier.get().get();
            SharedValues.customRenderedRegisterer().accept(entry, renderer);
            CustomRenderedItems.register(entry);
        }
    }

    @Environment(EnvType.CLIENT)
    private record CustomBlockItemRendererRegistrationHelper(
            Supplier<Supplier<CustomRenderedItemModelRenderer>> supplier) implements NonNullConsumer<BlockItem> {
        @Override
        public void accept(BlockItem entry) {
            CustomRenderedItemModelRenderer renderer = supplier.get().get();
            SharedValues.customBlockItemRenderedRegisterer().accept(entry, renderer);
            CustomRenderedItems.register(entry);
        }
    }
}