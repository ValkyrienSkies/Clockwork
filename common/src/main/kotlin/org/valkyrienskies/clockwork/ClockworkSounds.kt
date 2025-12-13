package org.valkyrienskies.clockwork

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.core.Registry
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.DataGenerator
import net.minecraft.data.DataProvider
import net.minecraft.data.HashCache
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.api.DeferredRegister
import java.io.IOException
import java.nio.file.Path
import java.util.function.Supplier

object ClockworkSounds {
    val ALL: MutableMap<ResourceLocation, SoundEntry> = HashMap()
    val PHYSICS_INFUSER_INITIALIZE = create("physics_infuser_initialize").subtitle("Physics Infuser starts")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val PHYSICS_INFUSER_WINDUP = create("physics_infuser_windup").subtitle("Physics Infuser windup")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val PHYSICS_INFUSER_LIGHTNING = create("physics_infuser_lightning").subtitle("Zap!")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val THICK_FLUID_SWIM = create("thick_fluid_swim").subtitle("Swimming through thick fluid")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val THICK_FLUID_FILL = create("thick_fluid_fill").subtitle("Filled with a thick fluid")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val THICK_FLUID_EMPTY = create("thick_fluid_empty").subtitle("Emptied a thick fluid")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val PHYSICS_INFUSER_FINISH = create("physics_infuser_finish").subtitle("Physics Infuser infuses")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(64)
        .build()
    val SUPERSONIC = create("supersonic").subtitle("Supersonic")
        .category(SoundSource.RECORDS)
        .attenuationDistance(32)
        .build()
    val WAND_EQUIP = create("wand_equip").subtitle("Wanderwand awakens")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val WAND_START = create("wand_start").subtitle("Wanderwand initializes")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val WAND_WELD = create("welder_whirr").subtitle("Wanderwand whirrs")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val WAND_IDLE = create("wand_idle").subtitle("Wanderwand drones")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val WAND_FINISH = create("wand_finish").subtitle("Wanderwand activates")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val BOING = create("boing").subtitle("Boioing!")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val DOINK = create("doink").subtitle("Doink.")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val TICK = create("tick").subtitle("Tick!")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val TOCK = create("tock").subtitle("Tock!")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val CLOCK_SONG = create("clock_song").subtitle("teaser moment")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(128)
        .build()
    val THWOOM = create("thwoom").subtitle("Thwoomp!")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val JUNK_PROPELLER = create("junk_propeller").subtitle("Jury-rigged propeller spins")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val PROPELLER = create("propeller").subtitle("Propeller spins")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val JUNK_RATTLE = create("junk_rattle").subtitle("Jury-rigged machinery rattles")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()
    val PROPELLER_START = create("propeller_start").subtitle("Propeller starting up")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val PROPELLER_STOP = create("propeller_stop").subtitle("Propeller spinning down")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val THRUSTER = create("thruster").subtitle("Gas thruster roar")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val GAS_HISS = create("gas_hiss").subtitle("Hissing gas")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(32)
        .build()
    val WANDERLUST = create("wanderlust").subtitle("'Wanderlust' plays")
        .category(SoundSource.RECORDS)
        .attenuationDistance(32)
        .build()
    val FLIGHTSUIT_EQUIP = create("flightsuit_equip").subtitle("Equipped aeronaut gear")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val FLIGHTSUIT_UNEQUIP = create("flightsuit_unequip").subtitle("Unequipped aeronaut gear")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val GOGGLES_EQUIP = create("goggles_equip").subtitle("Equipped aeronaut goggles")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val GOGGLES_UNEQUIP = create("goggles_unequip").subtitle("Unequipped aeronaut goggles")
        .category(SoundSource.PLAYERS)
        .attenuationDistance(16)
        .build()
    val GEAR_WHIRR = create("gear_whirr").subtitle("Gear whirring")
        .category(SoundSource.BLOCKS)
        .attenuationDistance(16)
        .build()

    private val sounds: DeferredRegister<SoundEvent> =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ClockworkMod.MOD_ID)

    private fun create(name: String): SoundEntryBuilder {
        return create(ClockworkMod.asResource(name))
    }

    fun create(id: ResourceLocation): SoundEntryBuilder {
        return SoundEntryBuilder(id)
    }

    @JvmStatic
    fun register() {
        for (entry in ALL.values) entry.register()
        sounds.registerAll()
    }

    fun provideLangEntries(): JsonObject {
        val `object` = JsonObject()
        for (entry in ALL.values) if (entry.hasSubtitle()) `object`.addProperty(
            entry.subtitleKey,
            entry.subtitle
        )
        return `object`
    }

    data class ConfiguredSoundEvent(val event: Supplier<SoundEvent>, val volume: Float, val pitch: Float)
    class SoundEntryBuilder(protected var id: ResourceLocation) {
        protected var subtitle: String? = "unregistered"
        protected var category = SoundSource.BLOCKS
        protected var wrappedEvents: MutableList<ConfiguredSoundEvent>
        protected var variants: MutableList<ResourceLocation>
        protected var attenuationDistance = 0

        init {
            wrappedEvents = ArrayList()
            variants = ArrayList()
        }

        fun subtitle(subtitle: String?): SoundEntryBuilder {
            this.subtitle = subtitle
            return this
        }

        fun attenuationDistance(distance: Int): SoundEntryBuilder {
            attenuationDistance = distance
            return this
        }

        fun noSubtitle(): SoundEntryBuilder {
            subtitle = null
            return this
        }

        fun category(category: SoundSource): SoundEntryBuilder {
            this.category = category
            return this
        }

        fun addVariant(name: String): SoundEntryBuilder {
            return addVariant(ClockworkMod.asResource(name))
        }

        fun addVariant(id: ResourceLocation): SoundEntryBuilder {
            variants.add(id)
            return this
        }

        fun playExisting(event: Supplier<SoundEvent>, volume: Float, pitch: Float): SoundEntryBuilder {
            wrappedEvents.add(ConfiguredSoundEvent(event, volume, pitch))
            return this
        }

        @JvmOverloads
        fun playExisting(event: SoundEvent, volume: Float = 1f, pitch: Float = 1f): SoundEntryBuilder {
            return playExisting({ event }, volume, pitch)
        }

        fun build(): SoundEntry {
            val entry = if (wrappedEvents.isEmpty()) CustomSoundEntry(
                id,
                variants,
                subtitle,
                category,
                attenuationDistance
            ) else WrappedSoundEntry(
                id, subtitle!!, wrappedEvents, category, attenuationDistance
            )
            ALL[entry.id] = entry
            return entry
        }
    }

    abstract class SoundEntry(
        var id: ResourceLocation,
        var subtitle: String?,
        protected var category: SoundSource,
        protected var attenuationDistance: Int
    ) {
        abstract fun register()
        abstract fun write(json: JsonObject?)
        abstract val mainEvent: SoundEvent?
        val subtitleKey: String
            get() = id.namespace + ".subtitle." + id.path

        fun hasSubtitle(): Boolean {
            return subtitle != null
        }

        @JvmOverloads
        fun playOnServer(world: Level?, pos: Vec3i, volume: Float = 1f, pitch: Float = 1f) {
            play(world, null, pos, volume, pitch)
        }

        @JvmOverloads
        fun playFrom(entity: Entity, volume: Float = 1f, pitch: Float = 1f) {
            if (!entity.isSilent) play(entity.level(), null, entity.blockPosition(), volume, pitch)
        }

        @JvmOverloads
        fun play(world: Level?, entity: Player?, pos: Vec3i, volume: Float = 1f, pitch: Float = 1f) {
            play(world, entity, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, volume, pitch)
        }

        fun play(world: Level?, entity: Player?, pos: Vec3, volume: Float, pitch: Float) {
            play(world, entity, pos.x(), pos.y(), pos.z(), volume, pitch)
        }

        abstract fun play(world: Level?, entity: Player?, x: Double, y: Double, z: Double, volume: Float, pitch: Float)
        fun playAt(world: Level?, pos: Vec3i, volume: Float, pitch: Float, fade: Boolean) {
            playAt(world, pos.x + .5, pos.y + .5, pos.z + .5, volume, pitch, fade)
        }

        fun playAt(world: Level?, pos: Vec3, volume: Float, pitch: Float, fade: Boolean) {
            playAt(world, pos.x(), pos.y(), pos.z(), volume, pitch, fade)
        }

        abstract fun playAt(world: Level?, x: Double, y: Double, z: Double, volume: Float, pitch: Float, fade: Boolean)
    }

    private class WrappedSoundEntry(
        id: ResourceLocation, subtitle: String,
        private val wrappedEvents: List<ConfiguredSoundEvent>, category: SoundSource, attenuationDistance: Int
    ) :
        SoundEntry(id, subtitle, category, attenuationDistance) {
        private val compiledEvents: MutableList<CompiledSoundEvent>

        init {
            compiledEvents = ArrayList()
        }

        override fun register() {
            for (i in wrappedEvents.indices) {
                val (_, volume, pitch) = wrappedEvents[i]
                val location = getIdOf(i)
                val event = SoundEvent.createVariableRangeEvent(location)
                compiledEvents.add(
                    CompiledSoundEvent(
                        event,
                        volume, pitch
                    )
                )
            }
            for ((event1) in compiledEvents) {
                sounds.register(
                    event1.location.path
                ) { event1 }
            }
        }

        override val mainEvent: SoundEvent
            get() = compiledEvents[0]
                .event

        protected fun getIdOf(i: Int): ResourceLocation {
            return ResourceLocation(id.namespace, if (i == 0) id.path else id.path + "_compounded_" + i)
        }

        override fun write(json: JsonObject?) {
            for (i in wrappedEvents.indices) {
                val (event1) = wrappedEvents[i]
                val entry = JsonObject()
                val list = JsonArray()
                val s = JsonObject()
                s.addProperty(
                    "name", event1
                        .get()
                        .location
                        .toString()
                )
                s.addProperty("type", "event")
                if (attenuationDistance != 0) s.addProperty("attenuation_distance", attenuationDistance)
                list.add(s)
                entry.add("sounds", list)
                if (i == 0 && hasSubtitle()) entry.addProperty("subtitle", subtitleKey)
                json!!.add(getIdOf(i).path, entry)
            }
        }

        override fun play(
            world: Level?,
            entity: Player?,
            x: Double,
            y: Double,
            z: Double,
            volume: Float,
            pitch: Float
        ) {
            for ((event1, volume1, pitch1) in compiledEvents) {
                world!!.playSound(
                    entity, x, y, z, event1, category, volume1 * volume,
                    pitch1 * pitch
                )
            }
        }

        override fun playAt(
            world: Level?,
            x: Double,
            y: Double,
            z: Double,
            volume: Float,
            pitch: Float,
            fade: Boolean
        ) {
            for ((event1, volume1, pitch1) in compiledEvents) {
                world!!.playLocalSound(
                    x, y, z, event1, category, volume1 * volume,
                    pitch1 * pitch, fade
                )
            }
        }

        private data class CompiledSoundEvent(val event: SoundEvent, val volume: Float, val pitch: Float)
    }

    private class CustomSoundEntry(
        id: ResourceLocation, protected var variants: List<ResourceLocation>, subtitle: String?,
        category: SoundSource, attenuationDistance: Int
    ) :
        SoundEntry(id, subtitle, category, attenuationDistance) {
        override var mainEvent: SoundEvent? = null
            protected set

        override fun register() {
            sounds.register(
                id.path,
                Supplier {
                    mainEvent = SoundEvent.createVariableRangeEvent(id)
                    return@Supplier mainEvent!!
                })
        }

        override fun write(json: JsonObject?) {
            val entry = JsonObject()
            val list = JsonArray()
            var s = JsonObject()
            s.addProperty("name", id.toString())
            s.addProperty("type", "file")
            if (attenuationDistance != 0) s.addProperty("attenuation_distance", attenuationDistance)
            list.add(s)
            for (variant in variants) {
                s = JsonObject()
                s.addProperty("name", variant.toString())
                s.addProperty("type", "file")
                if (attenuationDistance != 0) s.addProperty("attenuation_distance", attenuationDistance)
                list.add(s)
            }
            entry.add("sounds", list)
            if (hasSubtitle()) entry.addProperty("subtitle", subtitleKey)
            json!!.add(id.path, entry)
        }

        override fun play(
            world: Level?,
            entity: Player?,
            x: Double,
            y: Double,
            z: Double,
            volume: Float,
            pitch: Float
        ) {
            world!!.playSound(entity, x, y, z, mainEvent!!, category, volume, pitch)
        }

        override fun playAt(
            world: Level?,
            x: Double,
            y: Double,
            z: Double,
            volume: Float,
            pitch: Float,
            fade: Boolean
        ) {
            world!!.playLocalSound(x, y, z, mainEvent!!, category, volume, pitch, fade)
        }
    }
}
