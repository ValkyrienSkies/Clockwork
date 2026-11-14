package example.thinkable;

import net.fabricmc.api.ModInitializer;

public final class FabricFixture implements ModInitializer {
    @Override
    public void onInitialize() {
        new CompatFixture();
    }
}
