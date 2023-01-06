package org.valkyrienskies.clockwork.forge.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CWKinetics extends ConfigBase {

    public final ConfigInt propellorRPMPerSail = i(8, 1, "propellorRPMPerSail", Comments.propellorRPMPerSail);

    public final CWStress stressValues = nested(1, CWStress::new, Comments.stress);


    @Override
    public String getName() {
        return "cwkinetics";
    }

    private static class Comments {
        static String propellorRPMPerSail = "RPM input requirement per Sail attached.";
        static String stress = "Fine tune the kinetic stats of individual components";
    }

}
