package org.valkyrienskies.clockwork.effekseer.client.registry

import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.EffekseerEffect
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.EffekseerManager
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.ParticleEmitter
import java.io.Closeable
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.random.RandomGenerator
import java.util.stream.Stream
import kotlin.math.abs


/**
 * An effect wrapper with a registry name,
 * the effect instance is mutable.
 *
 * @author ChloePrime
 */
class EffectDefinition : Closeable {
    fun play(emitterName: ResourceLocation?): ParticleEmitter {
        return play(ParticleEmitter.Type.WORLD, emitterName)
    }

    @JvmOverloads
    fun play(type: ParticleEmitter.Type = ParticleEmitter.Type.WORLD): ParticleEmitter {
        val emitter = getManager(type)!!.createParticle(effect!!, type!!)
        Objects.requireNonNull(
            oneShotEmitters[type]
        )?.add(emitter)
        return emitter
    }

    fun play(type: ParticleEmitter.Type?, emitterName: ResourceLocation?): ParticleEmitter {
        val emitter = getManager(type)!!.createParticle(effect!!, type!!)
        val collection = Objects.requireNonNull(
            namedEmitters[type]
        )
        val old = collection?.put(emitterName, emitter)
        old?.stop()
        return emitter
    }

    fun getNamedEmitter(type: ParticleEmitter.Type?, emitterName: ResourceLocation?): Optional<ParticleEmitter> {
        return Optional.ofNullable(namedEmitters[type]!![emitterName])
    }

    fun getManager(type: ParticleEmitter.Type?): EffekseerManager? {
        return Objects.requireNonNull(managers[type])
    }

    fun emitters(): Stream<ParticleEmitter> {
        return emitterContainers().flatMap { obj: Collection<ParticleEmitter> -> obj.stream() }
    }

    fun emitters(type: ParticleEmitter.Type?): Stream<ParticleEmitter> {
        return emitterContainers(type).flatMap { obj: Collection<ParticleEmitter> -> obj.stream() }
    }

    fun emitterContainers(): Stream<MutableCollection<ParticleEmitter>> {
        return Stream.concat(
            oneShotEmitters.values.stream(),
            namedEmitters.values.stream()
                .map(Function<Map<ResourceLocation?, ParticleEmitter>, MutableCollection<ParticleEmitter>> { obj: Map<ResourceLocation?, ParticleEmitter> -> obj.values as MutableCollection<ParticleEmitter>? })
        )
    }

    fun emitterContainers(type: ParticleEmitter.Type?): Stream<Collection<ParticleEmitter>> {
        val oneshot = Objects.requireNonNull<Set<ParticleEmitter>>(oneShotEmitters[type])
        val named = Objects.requireNonNull<Map<ResourceLocation?, ParticleEmitter>>(
            namedEmitters[type]
        ).values
        return Stream.of(oneshot, named)
    }

    fun setEffect(effect: EffekseerEffect): EffectDefinition? {
        Objects.requireNonNull(effect)
        if (this.effect == effect) {
            return null
        }
        // If this is not the first time of load.
        if (this.effect != null) {
            emitters().forEach { obj: ParticleEmitter -> obj.stop() }
            managers().forEach { obj: EffekseerManager -> obj.close() }
            this.effect!!.close()
            managers.clear()
        }
        this.effect = effect
        initManager()
        return this
    }

    fun managers(): Stream<EffekseerManager> {
        return managers.values.stream()
    }

    /**
     * @apiNote Do not keep reference of its return value.
     * Actual effect may be updated upon resource pack reloads.
     *
     * @return the effect that can be played directly.
     */
    var effect: EffekseerEffect? = null
        private set
    private val managers = EnumMap<ParticleEmitter.Type, EffekseerManager>(
        ParticleEmitter.Type::class.java
    )
    private val oneShotEmitters = EnumMap<ParticleEmitter.Type, MutableSet<ParticleEmitter>>(
        ParticleEmitter.Type::class.java
    )
    private val namedEmitters = EnumMap<ParticleEmitter.Type, MutableMap<ResourceLocation?, ParticleEmitter>>(
        ParticleEmitter.Type::class.java
    )
    private val magicLoadBalancer = (abs((RNG.nextInt() ushr 2).toDouble()) % GC_DELAY).toInt()
    private var gcTicks = 0

    init {
        for (type in ParticleEmitter.Type.entries.toTypedArray()) {
            oneShotEmitters[type] = LinkedHashSet()
            namedEmitters[type] = LinkedHashMap()
        }
    }

    fun draw(
        type: ParticleEmitter.Type,
        w: Int,
        h: Int,
        camera: FloatArray,
        projection: FloatArray,
        deltaFrames: Float,
        partialTicks: Float
    ) {
        val manager: EffekseerManager = Objects.requireNonNull(managers[type])!!
        manager.setViewport(w, h)
        manager.setCameraMatrix(camera)
        manager.setProjectionMatrix(projection)
        manager.update(deltaFrames)
        emitters(type).forEach { emitter: ParticleEmitter ->
            emitter.runPreDrawCallbacks(
                partialTicks
            )
        }
        manager.draw()

        if (type == ParticleEmitter.Type.WORLD) {
            gcTicks = (gcTicks + 1) % GC_DELAY
            if (gcTicks === magicLoadBalancer) {
                emitterContainers().forEach { container: MutableCollection<ParticleEmitter> ->
                    container.removeIf { emitter: ParticleEmitter -> !emitter.exists() }
                }
            }
        }
    }

    private fun initManager() {
        for (type in ParticleEmitter.Type.entries.toTypedArray()) {
            val old = managers.put(type, EffekseerManager())
            Optional.ofNullable(old).ifPresent { obj: EffekseerManager -> obj.close() }
        }
        val worldManager = Objects.requireNonNull(managers[ParticleEmitter.Type.WORLD])
        val fpvMhManager = Objects.requireNonNull(managers[ParticleEmitter.Type.FIRST_PERSON_MAINHAND])
        val fpvOhManager = Objects.requireNonNull(managers[ParticleEmitter.Type.FIRST_PERSON_OFFHAND])
        check(worldManager!!.init(9000)) { "Failed to initialize EffekseerManager" }
        check(fpvMhManager!!.init(500)) { "Failed to initialize (fpv mainhand) EffekseerManager" }
        check(fpvOhManager!!.init(500)) { "Failed to initialize (fpv offhand) EffekseerManager" }
        worldManager.setupWorkerThreads(2)
        fpvMhManager.setupWorkerThreads(1)
        fpvOhManager.setupWorkerThreads(1)
    }

    override fun close() {
        managers.values.forEach(Consumer { obj: EffekseerManager -> obj.close() })
        effect!!.close()
    }

    companion object {
        private val RNG: RandomGenerator = Random()
        private const val GC_DELAY = 20
    }
}