package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.utility.AnimationTickHolder
import com.simibubi.create.foundation.utility.Pair
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.data.models.blockstates.PropertyDispatch.QuadFunction
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.util.sound.SoundScape
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*
import java.util.function.Consumer

object ClockworkSoundScapes {

    const val MAX_AMBIENT_SOURCE_DISTANCE: Int = 16

    const val UPDATE_INTERVAL: Int = 5

    const val SOUND_VOLUME_ARG_MAX: Int = 15

    enum class AmbienceGroup(private val factory: QuadFunction<Float, AmbienceGroup, Ship?, BlockPos?, SoundScape>) {
        RICKETY({ pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos? -> rickety(pitch, group, ship) }),
        PROPELLER({ pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos? -> propeller(pitch, group, ship, pos) }),
        JURYRIGGED_PROPELLER({ pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos? -> juryriggedPropeller(pitch, group, ship, pos) }),
        THRUSTER({ pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos? -> thruster(pitch, group, ship, pos) }),
        ;

        fun instantiate(pitch: Float, ship: Ship?, pos: BlockPos?): SoundScape {
            return factory.apply(pitch, this, ship, pos)
        }
    }



    private fun rickety(pitch: Float, group: AmbienceGroup, ship: Ship?): SoundScape {
        return SoundScape(pitch, group, ship).repeating(ClockworkSounds.JUNK_RATTLE.mainEvent!!, 1.5f, 1f, 30)
    }

    private fun propeller(pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos?): SoundScape {
        return SoundScape(pitch, group, ship).continuous(ClockworkSounds.PROPELLER.mainEvent!!, 2f, 1f, ship, pos)
    }

    private fun juryriggedPropeller(pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos?): SoundScape {
        return SoundScape(pitch, group, ship).continuous(ClockworkSounds.JUNK_PROPELLER.mainEvent!!, 4f, 1f, ship, pos)
            .repeating(ClockworkSounds.JUNK_RATTLE.mainEvent!!, 1.5f, 1f, 30)
    }

    private fun thruster(pitch: Float, group: AmbienceGroup, ship: Ship?, pos: BlockPos?): SoundScape {
        return SoundScape(pitch, group, ship).repeating(ClockworkSounds.THRUSTER.mainEvent!!, 3f, 1f, 0)
    }

    enum class PitchGroup {
        VERY_LOW, LOW, NORMAL, HIGH, VERY_HIGH
    }

    @JvmStatic
    private val counter: MutableMap<AmbienceGroup, MutableMap<PitchGroup, MutableSet<BlockPos>>> = IdentityHashMap()
    @JvmStatic
    private val activeSounds: MutableMap<Pair<AmbienceGroup, PitchGroup>, SoundScape> = HashMap()

    fun play(group: AmbienceGroup, pos: BlockPos, pitch: Float) {
        if (!AllConfigs.client().enableAmbientSounds.get()) return
        if (!outOfRange(pos)) addSound(group, pos, pitch)
    }

    fun tick() {
        activeSounds.values
            .forEach(Consumer { obj: SoundScape -> obj.tick() })

        if (AnimationTickHolder.getTicks() % UPDATE_INTERVAL != 0) return

        val disable = !AllConfigs.client().enableAmbientSounds.get()
        val iterator: MutableIterator<Map.Entry<Pair<AmbienceGroup, PitchGroup>, SoundScape>> = activeSounds.entries
            .iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val value = entry.value

            if (disable || getSoundCount(key.first, key.second) == 0) {
                value.remove()
                iterator.remove()
            }
        }

        counter.values
            .forEach(Consumer<Map<PitchGroup, MutableSet<BlockPos>>> { m: Map<PitchGroup, MutableSet<BlockPos>> ->
                m.values
                    .forEach(Consumer { obj: MutableSet<BlockPos> -> obj.clear() })
            })
    }

    private fun addSound(group: AmbienceGroup, pos: BlockPos, pitch: Float) {
        val groupFromPitch = getGroupFromPitch(pitch)
        val realPos = BlockPos(Minecraft.getInstance().player?.level?.toWorldCoordinates(pos.toJOMLD())?.toMinecraft() ?: Vec3.atLowerCornerOf(pos))
        val set = counter.computeIfAbsent(group) { ag: AmbienceGroup -> IdentityHashMap() }
            .computeIfAbsent(groupFromPitch) { pg: PitchGroup? -> HashSet() }
        set.add(pos)

        val ship = Minecraft.getInstance().level?.getShipManagingPos(pos)

        val pair: Pair<AmbienceGroup, PitchGroup> =
            Pair.of(group, groupFromPitch)
        activeSounds.computeIfAbsent(pair) {
            val soundScape = group.instantiate(pitch, ship, realPos)
            soundScape.play()
            soundScape
        }
    }

    fun invalidateAll() {
        counter.clear()
        activeSounds.forEach { (_: Pair<AmbienceGroup, PitchGroup>, sound: SoundScape) -> sound.remove() }
        activeSounds.clear()
    }

    private fun outOfRange(pos: BlockPos): Boolean {
        return !getCameraPos().closerThan(BlockPos(Minecraft.getInstance().player?.level?.toWorldCoordinates(pos.toJOMLD())?.toMinecraft() ?: Vec3.atLowerCornerOf(pos)), MAX_AMBIENT_SOURCE_DISTANCE.toDouble())
    }

    private fun getCameraPos(): BlockPos {
        val renderViewEntity = Minecraft.getInstance().cameraEntity
            ?: return BlockPos.ZERO
        val playerLocation = renderViewEntity.level.toWorldCoordinates(renderViewEntity.blockPosition().toJOMLD());
        return BlockPos(playerLocation.toMinecraft())
    }

    fun getSoundCount(group: AmbienceGroup?, pitchGroup: PitchGroup?): Int {
        return getAllLocations(group, pitchGroup).size
    }

    fun getAllLocations(group: AmbienceGroup?, pitchGroup: PitchGroup?): Set<BlockPos> {
        return counter.getOrDefault(group, emptyMap<PitchGroup, Set<BlockPos>>())
            .getOrDefault(pitchGroup, emptySet())
    }

    fun getGroupFromPitch(pitch: Float): PitchGroup {
        if (pitch < .70) return PitchGroup.VERY_LOW
        if (pitch < .90) return PitchGroup.LOW
        if (pitch < 1.10) return PitchGroup.NORMAL
        if (pitch < 1.30) return PitchGroup.HIGH
        return PitchGroup.VERY_HIGH
    }

    fun init() { }
}