package example.thinkable;

import net.minecraftforge.fml.common.Mod;

@Mod("thinkable_compat_fixture")
public final class CompatFixture {
    public static boolean initialized;

    public CompatFixture() {
        initialized = true;
    }
}
