package org.valkyrienskies.clockwork.content.forces

import com.simibubi.create.content.equipment.armor.DivingHelmetItem
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkDamageTypes
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBackTankItem
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.GasTag
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Applies atmospheric effects to players breathing gas from a balloon/blimp pocket
 * or from a worn [GasBackTankItem] paired with a Create [DivingHelmetItem].
 *
 * Uses Kelvin gas tags (`kelvin:toxic` and `kelvin:breathable`):
 * - Toxic gas -> Poison effect
 * - Breathable gas -> safe (a breathable backtank also protects against hazardous pockets)
 * - Neither toxic nor breathable -> depletes breath supply and suffocates the player
 */
object BalloonPocketGasEffects {
    private const val MIN_GAS_MASS = 1e-4
    private const val MIN_GAS_MOLES = 1e-4
    private const val MIN_SEALED_PRESSURE = 500.0
    private const val TOXIC_MOLE_FRACTION_THRESHOLD = 0.01
    private const val BREATHABLE_MOLE_FRACTION_THRESHOLD = 0.5
    private const val POISON_DURATION_TICKS = 75
    private const val POISON_REFRESH_THRESHOLD_TICKS = 25
    private const val SUFFOCATION_DAMAGE = 2.0f
    private const val BACKTANK_VOLUME = 0.75
    private const val BACKTANK_CONSUMPTION_INTERVAL_TICKS = 20L
    internal const val BACKTANK_CONSUMPTION_KG_PER_SECOND = 1.0 / 100.0 // Matches Create's 1 Air/sec at AirKgsToAirTicks = 100

    val TOXIC_TAG = GasTag(KelvinMod.asResourceLocation("toxic"))
    val BREATHABLE_TAG = GasTag(KelvinMod.asResourceLocation("breathable"))

    private val suffocatingAir = ConcurrentHashMap<UUID, Int>()

    enum class PocketGasEffect {
        NONE,
        BREATHABLE,
        TOXIC,
        SUFFOCATING,
    }

    fun clear() {
        suffocatingAir.clear()
    }

    fun tick(level: ServerLevel) {
        val players = level.players()
        if (players.isEmpty()) return

        for (player in players) {
            if (!player.isAlive || player.isSpectator || player.isCreative || player.isInvulnerable) {
                suffocatingAir.remove(player.uuid)
                continue
            }
            tickPlayer(level, player)
        }

        if (suffocatingAir.isNotEmpty()) {
            val playerList = level.server?.playerList
            if (playerList != null) {
                suffocatingAir.keys.removeIf { playerList.getPlayer(it) == null }
            }
        }
    }

    internal fun tickPlayer(
        level: ServerLevel,
        player: Player,
        toxicTag: GasTag = TOXIC_TAG,
        breathableTag: GasTag = BREATHABLE_TAG,
    ) {
        val pocket = findPocketForPlayer(level, player)
        val pocketEffect = if (pocket != null) {
            classifyPocket(pocket, toxicTag, breathableTag)
        } else {
            PocketGasEffect.NONE
        }

        val backtankStack = getActiveDivingBacktank(player)
        if (backtankStack != null) {
            val backtankEffect = classifyBacktank(backtankStack, toxicTag, breathableTag)
            if (backtankEffect != PocketGasEffect.NONE) {
                val shouldConsume = when (backtankEffect) {
                    PocketGasEffect.TOXIC,
                    PocketGasEffect.SUFFOCATING -> true
                    PocketGasEffect.BREATHABLE ->
                        pocketEffect == PocketGasEffect.TOXIC || pocketEffect == PocketGasEffect.SUFFOCATING
                    PocketGasEffect.NONE -> false
                }
                if (shouldConsume && level.gameTime % BACKTANK_CONSUMPTION_INTERVAL_TICKS == 0L) {
                    GasBackTankItem.consumeGas(backtankStack, BACKTANK_CONSUMPTION_KG_PER_SECOND, breathableTag)
                }
                applyEffect(level, player, backtankEffect, fromBacktank = true)
                return
            }
        }

        applyEffect(level, player, pocketEffect, fromBacktank = false)
    }

    internal fun getActiveDivingBacktank(player: Player): ItemStack? {
        if (!DivingHelmetItem.isWornBy(player)) return null
        return GasBackTankItem.getWornStack(player)
    }

    fun classifyBacktank(
        stack: ItemStack,
        toxicTag: GasTag = TOXIC_TAG,
        breathableTag: GasTag = BREATHABLE_TAG,
    ): PocketGasEffect {
        val masses = GasBackTankItem.readGasMasses(stack)
        if (masses.isEmpty()) return PocketGasEffect.NONE
        val temp = GasBackTankItem.readTemperature(stack)
        val energy = BalloonThermodynamics.capacity(masses) * temp
        return classifyGas(
            masses = masses,
            energy = energy,
            volume = BACKTANK_VOLUME,
            sealed = false,
            toxicTag = toxicTag,
            breathableTag = breathableTag,
        )
    }

    internal fun findPocketForPlayer(level: ServerLevel, player: Player): BalloonData? {
        val eye = player.eyePosition
        val eyeBox = AABB(eye.x - 0.1, eye.y - 0.1, eye.z - 0.1, eye.x + 0.1, eye.y + 0.1, eye.z + 0.1)

        for (ship in level.getShipsIntersecting(eyeBox)) {
            val loadedShip = level.shipObjectWorld.loadedShips.getById(ship.id) ?: continue
            if (loadedShip.chunkClaimDimension != level.dimensionId) continue
            val controller = loadedShip.getAttachment(BalloonController::class.java) ?: continue
            val shipPos = loadedShip.transform.worldToShip.transformPosition(Vector3d(eye.x, eye.y, eye.z))
            val blockPos = BlockPos.containing(shipPos.x, shipPos.y, shipPos.z)
            val balloonId = controller.getExistingBalloon(blockPos)
            if (balloonId != -1) {
                val balloon = controller.getBalloonById(balloonId)
                if (balloon != null && !balloon.shouldRemove && balloon.enclosureStatus != BalloonData.EnclosureStatus.UNKNOWN) {
                    return balloon
                }
            }
        }

        val directPos = BlockPos.containing(eye.x, eye.y, eye.z)
        val shipyardShip = level.getLoadedShipManagingPos(directPos)
        if (shipyardShip != null && shipyardShip.chunkClaimDimension == level.dimensionId) {
            val controller = shipyardShip.getAttachment(BalloonController::class.java)
            if (controller != null) {
                val balloonId = controller.getExistingBalloon(directPos)
                if (balloonId != -1) {
                    val balloon = controller.getBalloonById(balloonId)
                    if (balloon != null && !balloon.shouldRemove && balloon.enclosureStatus != BalloonData.EnclosureStatus.UNKNOWN) {
                        return balloon
                    }
                }
            }
        }

        return null
    }

    fun classifyPocket(
        balloon: BalloonData,
        toxicTag: GasTag = TOXIC_TAG,
        breathableTag: GasTag = BREATHABLE_TAG,
    ): PocketGasEffect {
        return classifyGas(
            masses = balloon.resolvedMasses(),
            energy = balloon.currentEnergy,
            volume = balloon.currentVolume,
            sealed = balloon.sealed,
            toxicTag = toxicTag,
            breathableTag = breathableTag,
        )
    }

    fun classifyGas(
        masses: Map<GasType, Double>,
        energy: Double,
        volume: Double,
        sealed: Boolean = false,
        toxicTag: GasTag = TOXIC_TAG,
        breathableTag: GasTag = BREATHABLE_TAG,
    ): PocketGasEffect {
        var totalMass = 0.0
        var totalMoles = 0.0
        var toxicMoles = 0.0
        var breathableMoles = 0.0

        for ((gas, mass) in masses) {
            if (!mass.isFinite() || mass <= 0.0) continue
            val moles = gas.massToMoles(mass)
            if (!moles.isFinite() || moles <= 0.0) continue
            totalMass += mass
            totalMoles += moles
            if (gas.hasTag(toxicTag)) toxicMoles += moles
            if (gas.hasTag(breathableTag)) breathableMoles += moles
        }

        if (totalMass <= MIN_GAS_MASS || totalMoles <= MIN_GAS_MOLES) {
            return PocketGasEffect.NONE
        }

        if (sealed && volume > 0.0) {
            val internalPressure = BalloonThermodynamics.pressure(masses, energy, volume)
            if (internalPressure < MIN_SEALED_PRESSURE) {
                return PocketGasEffect.NONE
            }
        }

        val toxicFraction = toxicMoles / totalMoles
        if (toxicFraction >= TOXIC_MOLE_FRACTION_THRESHOLD) {
            return PocketGasEffect.TOXIC
        }

        val breathableFraction = breathableMoles / totalMoles
        if (breathableFraction >= BREATHABLE_MOLE_FRACTION_THRESHOLD) {
            return PocketGasEffect.BREATHABLE
        }

        return PocketGasEffect.SUFFOCATING
    }

    internal fun applyEffect(
        level: ServerLevel,
        player: Player,
        effect: PocketGasEffect,
        fromBacktank: Boolean = false,
    ) {
        if (!player.isAlive || player.isSpectator || player.isCreative || player.isInvulnerable) {
            suffocatingAir.remove(player.uuid)
            return
        }

        when (effect) {
            PocketGasEffect.TOXIC -> {
                suffocatingAir.remove(player.uuid)
                val existing = player.getEffect(MobEffects.POISON)
                if (existing == null || (existing.amplifier == 0 && existing.duration <= POISON_REFRESH_THRESHOLD_TICKS)) {
                    player.addEffect(MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0))
                }
            }

            PocketGasEffect.SUFFOCATING -> {
                if (!fromBacktank && player.isEyeInFluid(FluidTags.WATER)) {
                    suffocatingAir.remove(player.uuid)
                    return
                }
                val uuid = player.uuid
                val tracked = suffocatingAir[uuid]
                val currentAir = if (tracked != null) minOf(tracked, player.airSupply) else player.airSupply
                var nextAir = currentAir - 1
                if (nextAir <= -20) {
                    nextAir = 0
                    player.hurt(ClockworkDamageTypes.gasSuffocation(level.registryAccess(), player), SUFFOCATION_DAMAGE)
                }
                player.airSupply = nextAir
                suffocatingAir[uuid] = nextAir
            }

            PocketGasEffect.BREATHABLE,
            PocketGasEffect.NONE -> {
                suffocatingAir.remove(player.uuid)
            }
        }
    }
}
