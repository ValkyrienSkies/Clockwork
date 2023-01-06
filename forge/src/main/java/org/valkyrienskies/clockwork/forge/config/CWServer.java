package org.valkyrienskies.clockwork.forge.config;

import com.simibubi.create.foundation.config.ConfigBase;

public class CWServer extends CWConfigBase {


    public final CWKinetics kinetics = nested(0, CWKinetics::new, Comments.kinetics);

    @Override
    public String getName() {
        return "server";
    }

    private static class Comments {
        static String kinetics = "Parameters and abilities of VS Clockwork's kinetic mechanisms";
    }

}
