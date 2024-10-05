package org.valkyrienskies.clockwork.platform

import com.google.common.base.Suppliers
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.Util
import java.io.File
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream


enum class NativePlatform @JvmOverloads constructor(
    val libraryFormat: String,
    private val prefix: String = "",
    val isUnsupported: Boolean = false
) {
    WINDOWS(".dll"),
    LINUX(".so", "lib"),
    MACOS(".dylib", "lib");

    fun getNativeInstallPath(dllName: String): File {
        return File(INSTALL_FOLDER.get(), formatFileName(dllName))
    }

    fun formatFileName(dllName: String): String {
        return prefix + dllName + libraryFormat
    }


    companion object {
        @JvmStatic
        val isRunningOnUnsupportedPlatform: Boolean
            get() = current().isUnsupported || isDataGen

        @JvmStatic
        @get:ExpectPlatform
        val isDataGen: Boolean
            get() {
                throw AbstractMethodError()
            }

        fun current(): NativePlatform {
            return CURRENT.get()
        }

        private val CURRENT: Supplier<NativePlatform> = Suppliers.memoize { findCurrent() }
        private val INSTALL_FOLDER: Supplier<File> = Suppliers.memoize { findNativeFolder() }
        fun findCurrent(): NativePlatform {
            return when (Util.getPlatform()) {
                Util.OS.LINUX -> LINUX
                Util.OS.SOLARIS -> throw UnsupportedOperationException("Solaris")
                Util.OS.WINDOWS -> WINDOWS
                Util.OS.OSX -> MACOS
                Util.OS.UNKNOWN -> throw UnsupportedOperationException("Unknown Platform")
            }
        }

        private fun findNativeFolder(): File {
            val root = File("./")
            return Optional.ofNullable(root.listFiles { dir: File, name: String ->
                if (!dir.isDirectory) {
                    return@listFiles false
                }
                "native" == name || "natives" == name || name.startsWith("native-") || name.startsWith("natives-")
            })
                .map<Stream<File>> { array: Array<File?>? ->
                    Arrays.stream(
                        array
                    )
                }
                .flatMap { obj: Stream<File> -> obj.findAny() }
                .orElse(root)
        }
    }
}