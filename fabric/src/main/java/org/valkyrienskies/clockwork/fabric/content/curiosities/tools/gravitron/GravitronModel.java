package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron;


import com.simibubi.create.foundation.item.render.CreateCustomRenderedItemModel;
import net.minecraft.client.resources.model.BakedModel;

public class GravitronModel extends CreateCustomRenderedItemModel {

    public GravitronModel(BakedModel template) {
        super(template, "gravitron");
        addPartials("gravicog", "gravidial", "gravileftprong", "gravirightprong", "gravitopprong", "gravileftprongtip", "gravirightprongtip", "gravitopprongtip");
    }

}
