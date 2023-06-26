package org.valkyrienskies.clockwork.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.datagen.ClockworkDataGen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class ClockworkDataGenFabric implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        Path cbcResources = Paths.get(System.getProperty(ExistingFileHelper.EXISTING_RESOURCES));
        ExistingFileHelper helper = new ExistingFileHelper(
                Set.of(cbcResources), Set.of("create"), true, null, null
        );
        ClockworkDataGen.register(generator, helper, true, true);
        ClockWorkMod.REGISTRATE.setupDatagen(generator, helper);
    }
}
