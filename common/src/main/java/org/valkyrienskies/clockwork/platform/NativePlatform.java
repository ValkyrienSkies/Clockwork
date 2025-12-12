package org.valkyrienskies.clockwork.platform;


import com.google.common.base.Suppliers;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.Util;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public enum NativePlatform {
    WINDOWS(".dll"),
    WINDOWS_ON_ARM(".dll", "", true),
    LINUX_X64(".so", "lib"),
    LINUX_NOT_X64(".so", "lib", true),
    MACOS_X64(".dylib", "lib"),
    MACOS_ARM(".dylib", "lib"),
    UNKNOWN(".so", "lib", true);

    @SuppressWarnings("ConstantValue")
    public static boolean isRunningOnUnsupportedPlatform() {
        return current().unsupported || isDataGen();
    }

    @ExpectPlatform
    public static boolean isDataGen() {
        throw new AbstractMethodError();
    }

    public static NativePlatform current() {
        return CURRENT.get();
    }

    public boolean isUnsupported() {
        return unsupported;
    }

    public File getNativeInstallPath(String dllName) {
        return new File(INSTALL_FOLDER.get(), formatFileName(dllName));
    }

    public String formatFileName(String dllName) {
        return prefix + dllName + libFormat;
    }

    public String getLibraryFormat() {
        return libFormat;
    }

    private static final Supplier<NativePlatform> CURRENT = Suppliers.memoize(NativePlatform::findCurrent);
    private static final Supplier<File> INSTALL_FOLDER = Suppliers.memoize(NativePlatform::findNativeFolder);
    private final String prefix;
    private final String libFormat;

    private final boolean unsupported;

    NativePlatform(String libFormat) {
        this(libFormat, "");
    }


    NativePlatform(String libFormat, String prefix) {
        this(libFormat, prefix, false);
    }

    NativePlatform(String libFormat, String prefix, boolean unsupported) {
        this.libFormat = libFormat;
        this.prefix = prefix;
        this.unsupported = unsupported;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public static NativePlatform findCurrent() {
        return switch (Util.getPlatform()) {
            case WINDOWS -> switch (System.getProperty("os.arch")) {
                case "aarch64" -> WINDOWS_ON_ARM;
                default -> WINDOWS;
            };
            case LINUX -> switch (System.getProperty("os.arch")) {
                case "x86", "amd64" -> LINUX_X64;
                default -> LINUX_NOT_X64;
            };
            case OSX -> switch (System.getProperty("os.arch")) {
                case "aarch64", "unknown" -> MACOS_ARM;
                default -> MACOS_X64;
            };
            default -> UNKNOWN;
        };
    }

    private static File findNativeFolder() {
        var root = new File("./");
        return Optional.ofNullable(root.listFiles((dir, name) -> {
                    if (!dir.isDirectory()) {
                        return false;
                    }
                    return "native".equals(name) || "natives".equals(name) || name.startsWith("native-") || name.startsWith("natives-");
                }))
                .map(Arrays::stream)
                .flatMap(Stream::findAny)
                .orElse(root);
    }
}