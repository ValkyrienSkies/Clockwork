package org.valkyrienskies.clockwork.effekseer.client.loader

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.EffekseerEffect
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.TextureType
import org.valkyrienskies.clockwork.effekseer.client.registry.EffectDefinition
import org.valkyrienskies.clockwork.effekseer.client.render.EffekRenderer.init
import org.valkyrienskies.clockwork.effekseer.common.util.LimitlessResourceLocation
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.function.BiConsumer
import java.util.function.IntFunction
import javax.annotation.Nullable


/**
 * Loading effects from minecraft's resource system.
 *
 * @author ChloePrime
 */
class EffekAssetLoader : SimplePreparableReloadListener<EffekAssetLoader.Preparations>() {
    fun get(id: ResourceLocation): EffectDefinition? {
        return loadedEffects[id]
    }

    fun entries(): Set<Map.Entry<ResourceLocation, EffectDefinition>> {
        return loadedEffects.entries
    }

    fun forEach(action: BiConsumer<ResourceLocation, EffectDefinition>?) {
        loadedEffects.forEach(action!!)
    }

    /**
     * @param name the minecraft asset path of .efkefc files.
     * @return the loaded effect.
     */
    private fun loadEffect(manager: ResourceManager, name: ResourceLocation): EffekseerEffect? {
        val modid = name.namespace
        val path = "effeks/" + name.path + ".efkefc"
        val assetLocation = ResourceLocation(modid, path)

        try {
            manager.getResource(assetLocation).use { efkefc ->
                val effect = EffekseerEffect()
                val success = effect.load(efkefc.inputStream, 1f)
                if (!success) {
                    throw EffekLoadException("Failed to load $assetLocation")
                }

                for (texType in TextureType.entries) {
                    val count = effect.textureCount(texType!!)
                    load(
                        manager,
                        name, count,
                        { i -> effect.getTexturePath(i, texType!!) },
                        { b, len, i -> effect.loadTexture(b, len, i, texType!!) }
                    )
                }
                load(
                    manager,
                    name,
                    effect.modelCount(),
                    { index: Int -> effect.getModelPath(index) },
                    effect::loadModel
                )
                load(
                    manager,
                    name,
                    effect.curveCount(),
                    { index: Int -> effect.getCurvePath(index) },
                    effect::loadCurve
                )
                load(
                    manager,
                    name,
                    effect.materialCount(),
                    { index: Int -> effect.getMaterialPath(index) },
                    effect::loadMaterial
                )
                return effect
            }
        } catch (ex: IOException) {
            handleCheckedException(ex)
            return null
        }
    }

    @Throws(IOException::class)
    private fun load(
        manager: ResourceManager,
        name: ResourceLocation, count: Int,
        pathGetter: IntFunction<String>,
        loadMethod: TriConsumer<ByteArray>
    ) {
        val modid = name.namespace
        for (i in 0 until count) {
            // example: "Texture/a.png" in 'sample.efkefc' from mod 'examplemod'
            val effekAssetPath = pathGetter.apply(i)
            val mcAssetPath = "effeks/$effekAssetPath"
                .replace('\\', '/')
                .replace("//", "/")
            val fallbackMcAssetPath = ("effeks/" + name.path + "/" + effekAssetPath)
                .replace('\\', '/')
                .replace("//", "/")

            // example: "examplemod:effeks/Texture/a.png"
            val main: LimitlessResourceLocation = LimitlessResourceLocation(modid, mcAssetPath)
            val fallback: LimitlessResourceLocation = LimitlessResourceLocation(modid, fallbackMcAssetPath)
            // Load from disk.
            val resource = getResourceOrUseFallbackPath(manager, main, fallback)
            resource.inputStream.use { input ->
                val bytes: ByteArray = IOUtils.toByteArray(input)
                val success = loadMethod.accept(bytes, bytes.size, i)
                if (!success) {
                    val info = String.format("Failed to load effek data %s", effekAssetPath)
                    LOGGER.debug(
                        String.format(
                            "\n%s\nmc asset path is \"%s\"",
                            info,
                            mcAssetPath
                        )
                    )
                    throw EffekLoadException(info)
                }
            }
        }
    }

    class Preparations {
        val loadedEffects: MutableMap<ResourceLocation, EffectDefinition> =
            LinkedHashMap<ResourceLocation, EffectDefinition>()
    }

    private fun unloadAll() {
        loadedEffects.forEach(BiConsumer<ResourceLocation, EffectDefinition> { id: ResourceLocation?, definition: EffectDefinition -> definition.close() })
        loadedEffects.clear()
    }

    override fun prepare(manager: ResourceManager, profilerFiller: ProfilerFiller): Preparations? {
        return null
    }

    override fun apply(prep_: Preparations, manager: ResourceManager, profilerFiller: ProfilerFiller) {
        init()
        val prep = Preparations()
        for (effek in manager.listResources(
            "effeks"
        ) { s: String -> s.endsWith(".efkefc") }) {
            val name = createEffekName(effek!!)
            prep.loadedEffects[name] = EffectDefinition().setEffect(loadEffect(manager, name)!!)!!
        }
        unloadAll()
        loadedEffects.putAll(prep.loadedEffects)
        INSTANCE = this
    }

    private fun interface TriConsumer<T> {
        /**
         * Function object for methods like [EffekseerEffect.loadModel]
         *
         * @param bytes  arg0
         * @param length arg2
         * @param index  arg2
         * @return success or fail
         * @see .load
         */
        fun accept(bytes: T, length: Int, index: Int): Boolean
    }

    private val loadedEffects: MutableMap<ResourceLocation, EffectDefinition> =
        LinkedHashMap<ResourceLocation, EffectDefinition>()

    companion object {
        private var INSTANCE: EffekAssetLoader? = null
        fun get(): EffekAssetLoader? {
            return INSTANCE
        }

        @JvmStatic
        @Throws(IOException::class)
        private fun getResourceOrUseFallbackPath(
            manager: ResourceManager,
            path: ResourceLocation,
            fallback: ResourceLocation
        ): Resource {
            return try {
                manager.getResource(path)
            } catch (ignored: FileNotFoundException) {
                manager.getResource(fallback)
            }
        }

        @JvmStatic
        private fun createEffekName(location: ResourceLocation): ResourceLocation {
            var filePath = location.path
            if (filePath.startsWith("effeks/")) {
                filePath = filePath.substring("effeks/".length)
            }
            if (filePath.endsWith(".efkefc") || filePath.endsWith(".efkpkg")) {
                filePath = filePath.substring(0, filePath.length - ".efkefc".length)
            }
            return ResourceLocation(location.namespace, filePath)
        }

        @JvmStatic
        private fun handleCheckedException(e: Exception) {
            throw RuntimeException(e)
        }

        private val LOGGER: Logger = LogManager.getLogger(
            EffekAssetLoader::class.java.simpleName
        )
    }
}