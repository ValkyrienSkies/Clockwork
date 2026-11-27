package org.valkyrienskies.clockwork.content.logistics.gas.particles

import net.createmod.catnip.math.VecHelper
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SimpleAnimatedParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Create-styled air particle for Kelvin's `kelvin:air` gas.
 *
 * Matches Create's `AirParticle` look (translucent 0.25 alpha, 0.75x scale, generic_7..0 sprites,
 * helical twirl) while consuming per-tick velocity `(xSpeed, ySpeed, zSpeed)` like Kelvin's gas
 * emitters rather than Create's origin-to-target displacement vector.
 */
class KelvinAirParticle(
    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    xSpeed: Double,
    ySpeed: Double,
    zSpeed: Double,
    sprites: SpriteSet
) : SimpleAnimatedParticle(level, x, y, z, sprites, 0f) {

    private var centerX: Double = x
    private var centerY: Double = y
    private var centerZ: Double = z

    private var velX: Double
    private var velY: Double
    private var velZ: Double

    private val twirlRadius: Float
    private val twirlAngleOffset: Float
    private val twirlAxis: Direction.Axis

    init {
        quadSize *= SIZE_MULTIPLIER
        hasPhysics = false
        setPos(x, y, z)
        xo = x
        yo = y
        zo = z

        val speedMag = sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed)
        val jitter = max(speedMag * VELOCITY_JITTER, MIN_JITTER)
        velX = xSpeed + (random.nextDouble() - 0.5) * 2.0 * jitter
        velY = ySpeed + (random.nextDouble() - 0.5) * 2.0 * jitter
        velZ = zSpeed + (random.nextDouble() - 0.5) * 2.0 * jitter

        twirlRadius = random.nextFloat() / TWIRL_RADIUS_DIVISOR
        twirlAngleOffset = random.nextFloat() * 360f
        twirlAxis = if (random.nextBoolean()) Direction.Axis.X else Direction.Axis.Z

        lifetime = (lifetime * LIFETIME_MULT).toInt().coerceAtLeast(1)
        setAlpha(BASE_ALPHA)
        setSpriteFromAge(sprites)
    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z
        if (age++ >= lifetime) {
            remove()
            return
        }

        centerX += velX
        centerY += velY
        centerZ += velZ
        velX *= DRAG
        velY *= DRAG
        velZ *= DRAG
        velY += computeThermalLift(velX, velY, velZ)

        val progress = age.toFloat() / lifetime.toFloat()
        val twirlOffset = computeTwirlOffset(age, twirlRadius, twirlAngleOffset, twirlAxis)

        val nextX = centerX + twirlOffset.x
        val nextY = centerY + twirlOffset.y
        val nextZ = centerZ + twirlOffset.z

        xd = nextX - x
        yd = nextY - y
        zd = nextZ - z

        setAlpha(computeAlpha(progress))
        setSpriteFromAge(sprites)
        move(xd, yd, zd)
    }

    override fun getLightColor(partialTick: Float): Int {
        val blockPos = BlockPos.containing(x, y, z)
        return if (level.hasChunkAt(blockPos)) {
            LevelRenderer.getLightColor(level, blockPos)
        } else {
            0
        }
    }

    companion object {
        const val BASE_ALPHA = 0.25f
        const val SIZE_MULTIPLIER = 0.75f
        const val DRAG = 0.94
        const val THERMAL_LIFT = 0.02
        const val BUOYANCY_GATING_K = 200.0
        const val LIFETIME_MULT = 1.75
        private const val TWIRL_RADIUS_DIVISOR = 6f
        private const val TWIRL_DEGREES_PER_TICK = 18f
        private const val TWIRL_RAMP_TICKS = 6f
        private const val FADE_OUT_START = 0.75f
        private const val VELOCITY_JITTER = 0.15
        private const val MIN_JITTER = 0.005

        /**
         * Matches `DefaultGasParticle`'s neutral-density thermal lift: at low speeds particles rise
         * cleanly out of exhausts/vents, while at high jet velocities inertia dominates so
         * horizontal thrusters/exhausts do not sharply bend upward.
         */
        fun computeThermalLift(velX: Double, velY: Double, velZ: Double): Double {
            val speedSq = velX * velX + velY * velY + velZ * velZ
            val buoyancyFactor = 1.0 / (1.0 + BUOYANCY_GATING_K * speedSq)
            return THERMAL_LIFT * buoyancyFactor
        }

        fun computeTwirlOffset(
            age: Int,
            twirlRadius: Float,
            twirlAngleOffset: Float,
            twirlAxis: Direction.Axis
        ): Vec3 {
            val angle = (age * TWIRL_DEGREES_PER_TICK + twirlAngleOffset) % 360f
            val ramp = (age / TWIRL_RAMP_TICKS).coerceIn(0f, 1f)
            return VecHelper.rotate(
                Vec3(0.0, (twirlRadius * ramp).toDouble(), 0.0),
                angle.toDouble(),
                twirlAxis
            )
        }

        fun computeAlpha(progress: Float): Float {
            return if (progress < FADE_OUT_START) {
                BASE_ALPHA
            } else {
                (BASE_ALPHA * ((1f - progress) / (1f - FADE_OUT_START))).coerceAtLeast(0f)
            }
        }
    }
}

class KelvinAirParticleProvider(private val spriteSet: SpriteSet) : ParticleProvider<KelvinAirParticleData> {
    override fun createParticle(
        type: KelvinAirParticleData,
        level: ClientLevel,
        x: Double,
        y: Double,
        z: Double,
        xSpeed: Double,
        ySpeed: Double,
        zSpeed: Double
    ): Particle {
        return KelvinAirParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet)
    }
}
