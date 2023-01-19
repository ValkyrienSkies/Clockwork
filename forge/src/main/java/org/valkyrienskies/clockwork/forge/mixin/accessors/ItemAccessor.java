package org.valkyrienskies.clockwork.forge.mixin.accessors;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemAccessor {

    @Accessor
    void setRenderProperties(Object renderProperties);

}
