package org.valkyrienskies.clockwork.forge.mixin;

import net.minecraft.world.item.Item;
import net.minecraftforge.client.IItemRenderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.forge.mixinducks.RenderPropertiesHolder;
import org.valkyrienskies.clockwork.platform.CWItem;

import java.util.function.Consumer;

@Mixin(CWItem.class)
public class MixinCWItem extends Item implements RenderPropertiesHolder {
    @Unique
    private IItemRenderProperties renderProperties;

    public MixinCWItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        if (renderProperties != null)
            consumer.accept(renderProperties);
        else
            super.initializeClient(consumer);
    }

    @Override
    public IItemRenderProperties getRenderProperties() {
        return renderProperties;
    }

    @Override
    public void setRenderProperties(IItemRenderProperties renderProperties) {
        this.renderProperties = renderProperties;
    }
}
