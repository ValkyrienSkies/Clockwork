package org.valkyrienskies.clockwork.platform.forge;

import net.minecraftforge.fml.ModLoader;

@SuppressWarnings("unused")
public class NativePlatformImpl {
    public static boolean isDataGen() {
        return ModLoader.isDataGenRunning() ;
    }
}