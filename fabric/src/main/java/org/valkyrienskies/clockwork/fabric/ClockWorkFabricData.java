package org.valkyrienskies.clockwork.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.ClockworkMod;

public class ClockWorkFabricData implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        ExistingFileHelper helper = ExistingFileHelper.withResourcesFromArg();
        ClockworkMod.INSTANCE.getREGISTRATE().setupDatagen(generator, helper);
        ClockworkModFabric.gatherData(generator, helper);
    }
}
