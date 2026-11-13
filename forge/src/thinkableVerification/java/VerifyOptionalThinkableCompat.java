import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class VerifyOptionalThinkableCompat {
    public static void main(String[] args) throws Exception {
        try (var loader = new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()}, VerifyOptionalThinkableCompat.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("org.valkyrienskies.clockwork.")) throw new ClassNotFoundException(name);
                return super.loadClass(name, resolve);
            }
        }) {
            var forge = Class.forName("example.thinkable.CompatFixture", true, loader);
            forge.getConstructor().newInstance();
            if (!forge.getField("initialized").getBoolean(null)) throw new AssertionError("Forge fixture did not initialize");
            var fabric = Class.forName("example.thinkable.FabricFixture", true, loader);
            fabric.getMethod("onInitialize").invoke(fabric.getConstructor().newInstance());
            try {
                Class.forName("example.thinkable.OptionalThinkableCompat", true, loader);
                throw new AssertionError("Clockwork API was visible to the isolated addon");
            } catch (NoClassDefFoundError expected) {
                if (!expected.getMessage().contains("ThinkableEntrypoint")) throw expected;
            }
        }
        System.out.println("Forge and Fabric addon entrypoints initialized with Clockwork classes unavailable.");
    }
}
