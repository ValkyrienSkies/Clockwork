package org.valkyrienskies.clockwork.effekseer.client.installer

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.*


/**
 * @author ChloePrime
 */
object JarExtractor {
    @Throws(IOException::class)
    fun extract(from: String?, targetFile: File?) {
        Objects.requireNonNull(JarExtractor::class.java.classLoader.getResource(from)).openStream().use { input ->
            FileUtils.openOutputStream(targetFile).use { output ->
                input.transferTo(output)
            }
        }
    }

    @Throws(IOException::class)
    fun update(resource: String, target: File): Boolean {
        var isUpToDate: Boolean
        getResource(resource).use { res ->
            Files.newInputStream(target.toPath()).use { fs ->
                val resHash = DigestUtils.sha1(res)
                val fileHash: ByteArray = DigestUtils.sha1(fs)
                isUpToDate = Arrays.equals(resHash, fileHash)
            }
        }
        if (isUpToDate) {
            return false
        }
        if (!target.delete()) {
            throw IOException("Failed to delete %s".formatted(target.getCanonicalPath()))
        }
        extract(resource, target)
        return true
    }

    @Throws(IOException::class)
    private fun getResource(resource: String): InputStream {
        return Objects.requireNonNull(JarExtractor::class.java.classLoader.getResource(resource)).openStream()
    }
}