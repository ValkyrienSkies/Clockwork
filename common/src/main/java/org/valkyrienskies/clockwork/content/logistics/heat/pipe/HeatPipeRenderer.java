package org.valkyrienskies.clockwork.content.logistics.heat.pipe;

import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class HeatPipeRenderer extends SmartBlockEntityRenderer<HeatPipeBlockEntity> {
    public HeatPipeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
