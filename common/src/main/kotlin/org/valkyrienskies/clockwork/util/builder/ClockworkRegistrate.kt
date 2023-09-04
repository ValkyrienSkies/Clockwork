package org.valkyrienskies.clockwork.util.builder

import com.simibubi.create.CreateClient
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.CustomRenderedItems
import com.simibubi.create.foundation.utility.RegisteredObjects
import com.tterrag.registrate.builders.ItemBuilder
import com.tterrag.registrate.util.nullness.NonNullConsumer
import com.tterrag.registrate.util.nullness.NonNullFunction
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.BlockItem
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.platform.Dist.onClient
import org.valkyrienskies.clockwork.platform.SharedValues.customBlockItemRenderedRegisterer
import org.valkyrienskies.clockwork.platform.SharedValues.customRenderedRegisterer
import java.util.function.Supplier

object ClockworkRegistrate {
    fun <T : CWItem, P> customRenderedItem(
        supplier: () -> () -> CustomRenderedItemModelRenderer
    ): NonNullUnaryOperator<ItemBuilder<T, P>> {
        return NonNullUnaryOperator { b: ItemBuilder<T, P> ->
            onClient {
                customRenderedItem(
                    b,
                    supplier
                )
            }
            b
        }
    }

    fun <T : BlockItem, P> customRenderedBlockItem(
        supplier: () -> () -> CustomRenderedItemModelRenderer
    ): NonNullFunction<ItemBuilder<T, P>, P> {
        return NonNullFunction { b: ItemBuilder<T, P> ->
            onClient {
                customRenderedBlockItem(
                    b,
                    supplier
                )
            }
            b.build()
        }
    }

    @Environment(EnvType.CLIENT)
    private fun <T : CWItem, P> customRenderedItem(
        b: ItemBuilder<T, P>,
        supplier: () -> () -> CustomRenderedItemModelRenderer
    ) {
        b.onRegister(CustomRendererRegistrationHelper(supplier))
    }

    @Environment(EnvType.CLIENT)
    private fun <T : BlockItem, P> customRenderedBlockItem(
        b: ItemBuilder<T, P>,
        supplier: () -> () -> CustomRenderedItemModelRenderer
    ) {
        b.onRegister(CustomBlockItemRendererRegistrationHelper(supplier))
    }

    @Environment(EnvType.CLIENT)
    private fun registerItemModel(
        entry: CWItem,
        func: Supplier<NonNullFunction<BakedModel, out BakedModel>>
    ) {
        CreateClient.MODEL_SWAPPER.customItemModels
            .register(RegisteredObjects.getKeyOrThrow(entry), func.get())
    }

    @Environment(EnvType.CLIENT)
    private fun registerCustomRenderedBlockItem(
        entry: BlockItem,
        func: Supplier<NonNullFunction<BakedModel, out BakedModel>>
    ) {
        CreateClient.MODEL_SWAPPER.customItemModels
            .register(RegisteredObjects.getKeyOrThrow(entry), func.get())
    }

    @Environment(EnvType.CLIENT)
    private data class CustomRendererRegistrationHelper(val supplier: () -> () -> CustomRenderedItemModelRenderer) :
        NonNullConsumer<CWItem> {
        override fun accept(entry: CWItem) {
            val renderer = supplier.invoke().invoke()
            customRenderedRegisterer().accept(entry, renderer)
            CustomRenderedItems.register(entry)
        }
    }

    @Environment(EnvType.CLIENT)
    private data class CustomBlockItemRendererRegistrationHelper(val supplier: () -> () -> CustomRenderedItemModelRenderer) :
        NonNullConsumer<BlockItem> {
        override fun accept(entry: BlockItem) {
            val renderer = supplier.invoke().invoke()
            customBlockItemRenderedRegisterer().accept(entry, renderer)
            CustomRenderedItems.register(entry)
        }
    }
}