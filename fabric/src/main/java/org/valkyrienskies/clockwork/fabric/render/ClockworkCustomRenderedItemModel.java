package org.valkyrienskies.clockwork.fabric.render;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import net.minecraft.client.resources.model.BakedModel;
import org.valkyrienskies.clockwork.ClockWorkMod;

public abstract class ClockworkCustomRenderedItemModel extends CustomRenderedItemModel {
    public ClockworkCustomRenderedItemModel(BakedModel template, String basePath) {
        super(template, ClockWorkMod.MOD_ID, basePath);
    }

}
