package org.valkyrienskies.clockwork.content.curiosities.particles

import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.equipment.bell.BasicParticleData
import com.simibubi.create.content.equipment.bell.BasicParticleData.IBasicParticleFactory
import com.simibubi.create.content.equipment.bell.CustomRotationParticle
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkParticles
import org.valkyrienskies.clockwork.util.sterner.SternerCopiumUtils

class PhysLightningParticle(
    worldIn: ClientLevel?,
    x: Double,
    y: Double,
    z: Double,
    vx: Double,
    vy: Double,
    vz: Double,
    private val animatedSprite: SpriteSet,
    data: ParticleOptions
) :
    CustomRotationParticle(worldIn, x, y, z, animatedSprite, 0f) {
    protected var startTicks: Int
    protected var endTicks: Int
    protected var numLoops: Int
    protected var firstStartFrame = 0
    protected var startFrames = 17
    protected var firstLoopFrame = 17
    protected var loopFrames = 16
    protected var firstEndFrame = 33
    protected var endFrames = 20
    protected var animationStage: AnimationStage?
    protected var totalFrames = 53
    protected var ticksPerFrame = 2

    init {
        quadSize = 0.5f
        setSize(quadSize, quadSize)
        loopLength = loopFrames + (random.nextFloat() * 5f - 4f).toInt()
        startTicks = startFrames + (random.nextFloat() * 5f - 4f).toInt()
        endTicks = endFrames + (random.nextFloat() * 5f - 4f).toInt()
        numLoops = (1f + random.nextFloat() * 2f).toInt()
        setFrame(0)
        mirror = random.nextBoolean()
        animationStage = StartAnimation(this)
    }

    override fun tick() {
        animationStage!!.tick()
        animationStage = animationStage!!.next
        if (animationStage == null) remove()
    }

    override fun render(builder: VertexConsumer, camera: Camera, partialTicks: Float) {
        super.render(builder, camera, partialTicks)
    }

    fun setFrame(frame: Int) {
        if (frame >= 0 && frame < totalFrames) setSprite(animatedSprite[frame, totalFrames])
    }

    override fun getCustomRotation(camera: Camera, partialTicks: Float): Quaternionf {
        return SternerCopiumUtils.oldQuaternionf(0f, 0f, 0f, true)
    }

    class Data : BasicParticleData<PhysLightningParticle>() {
        override fun getBasicFactory(): IBasicParticleFactory<PhysLightningParticle> {
            return IBasicParticleFactory { worldIn: ClientLevel?, x: Double, y: Double, z: Double, vx: Double, vy: Double, vz: Double, spriteSet: SpriteSet ->
                PhysLightningParticle(
                    worldIn, x, y, z, vx, vy, vz,
                    spriteSet, this
                )
            }
        }

        override fun getType(): ParticleType<*> {
            return ClockworkParticles.PHYS_LIGHTNING.get()
        }
    }

    abstract class AnimationStage(protected val particle: PhysLightningParticle) {
        protected var ticks = 0
        protected var animAge = 0
        open fun tick() {
            ticks++
            if (ticks % particle.ticksPerFrame == 0) animAge++
        }

        fun getAnimAge(): Float {
            return animAge.toFloat()
        }

        abstract val next: AnimationStage?
    }

    class StartAnimation(particle: PhysLightningParticle) : AnimationStage(particle) {
        override fun tick() {
            super.tick()
            particle.setFrame(
                particle.firstStartFrame + (getAnimAge() / particle.startTicks.toFloat() * particle.startFrames).toInt()
            )
        }

        override val next: AnimationStage?
            get() = if (animAge < particle.startTicks) this else LoopAnimation(
                particle
            )
    }

    class LoopAnimation(particle: PhysLightningParticle) : AnimationStage(particle) {
        var loops = 0
        override fun tick() {
            super.tick()
            val loopTick = loopTick
            if (loopTick == 0) loops++
            particle.setFrame(particle.firstLoopFrame + loopTick) // (int) (((float) loopTick / (float)
            // particle.loopLength) * particle.loopFrames));
        }

        private val loopTick: Int
            private get() = animAge % particle.loopFrames
        override val next: AnimationStage?
            get() {
                return if (loops <= particle.numLoops) this else EndAnimation(
                    particle
                )
            }
    }

    class EndAnimation(particle: PhysLightningParticle) : AnimationStage(particle) {
        override fun tick() {
            super.tick()
            particle.setFrame(
                particle.firstEndFrame + (getAnimAge() / particle.endTicks.toFloat() * particle.endFrames).toInt()
            )
        }

        override val next: AnimationStage?
            get() {
                return if (animAge < particle.endTicks) this else null
            }
    }
}