package org.valkyrienskies.clockwork.util.builder;

import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.BlockItem;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.clockwork.platform.Dist;
import org.valkyrienskies.clockwork.platform.SharedValues;

import java.util.function.Supplier;

public class ClockworkRegistrate {

    public static <T extends CWItem, P> NonNullUnaryOperator<ItemBuilder<T, P>> customRenderedItem(
            Supplier<Supplier<CustomRenderedItemModelRenderer<?>>> supplier) {
        return b -> {
            Dist.onClient(() -> customRenderedItem(b, supplier));
            return b;
        };
    }

    public static <T extends BlockItem, P> NonNullFunction<ItemBuilder<T, P>, P> customRenderedBlockItem(
            Supplier<Supplier<CustomRenderedItemModelRenderer<?>>> supplier) {
        return b -> {
            Dist.onClient(() -> customRenderedBlockItem(b, supplier));
            return b.build();
        };
    }

    @Environment(EnvType.CLIENT)
    private static <T extends CWItem, P> void customRenderedItem(ItemBuilder<T, P> b,
                                                                 Supplier<Supplier<CustomRenderedItemModelRenderer<?>>> supplier) {
        b.onRegister(new CustomRendererRegistrationHelper(supplier));
    }

    @Environment(EnvType.CLIENT)
    private static <T extends BlockItem, P> void customRenderedBlockItem(ItemBuilder<T, P> b,
                                                                 Supplier<Supplier<CustomRenderedItemModelRenderer<?>>> supplier) {
        b.onRegister(new CustomBlockItemRendererRegistrationHelper(supplier));
    }

    @Environment(EnvType.CLIENT)
    private static void registerCustomRenderedItem(CWItem entry, CustomRenderedItemModelRenderer<?> renderer) {
        CreateClient.MODEL_SWAPPER.getCustomRenderedItems()
                .register(RegisteredObjects.getKeyOrThrow(entry), renderer::createModel);
    }

    @Environment(EnvType.CLIENT)
    private static void registerCustomRenderedBlockItem(BlockItem entry, CustomRenderedItemModelRenderer<?> renderer) {
        CreateClient.MODEL_SWAPPER.getCustomRenderedItems()
                .register(RegisteredObjects.getKeyOrThrow(entry), renderer::createModel);
    }

    @Environment(EnvType.CLIENT)
    private record CustomRendererRegistrationHelper(
            Supplier<Supplier<CustomRenderedItemModelRenderer<?>>> supplier) implements NonNullConsumer<CWItem> {
        @Override
        public void accept(CWItem entry) {
            CustomRenderedItemModelRenderer<?> renderer = supplier.get().get();
            SharedValues.customRenderedRegisterer().accept(entry, renderer);
            registerCustomRenderedItem(entry, renderer);
        }
    }

    @Environment(EnvType.CLIENT)
    private record CustomBlockItemRendererRegistrationHelper(
            Supplier<Supplier<CustomRenderedItemModelRenderer<?>>> supplier) implements NonNullConsumer<BlockItem> {
        @Override
        public void accept(BlockItem entry) {
            CustomRenderedItemModelRenderer<?> renderer = supplier.get().get();
            SharedValues.customBlockItemRenderedRegisterer().accept(entry, renderer);
            registerCustomRenderedBlockItem(entry, renderer);
        }
    }
}
