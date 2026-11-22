package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.MudBlock
import net.minecraft.world.level.block.SnowLayerBlock
import org.joml.Vector3f
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.AABB
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4f
import org.joml.Matrix4d
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import org.valkyrienskies.core.api.bodies.ClientVsBody
import org.valkyrienskies.core.api.bodies.shape.WheelBodyShapeData
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

object WheelEffects {
    class Coating {
        var amount = 0f
        var state: BlockState = Blocks.GRASS_BLOCK.defaultBlockState()
        val color = Vector3f(1f)
        var trail = 0
    }
    private class Wheel(var state: TireState, var time: Long) {
        val coating = Coating()
        var trackPoint: Vector3d? = null
        var trackSurface = -1L
        var trackDistance = 0.0
        var material = state.effect?.material ?: WheelSurface.HARD
        var candidate = material
        var candidateTicks = 0
        var heat = 0f
        var dust = 0.0
        var smoke = 0.0
        var load = 0f
        var airborne = 0
        var lastImpact = -100L
        var effectTime = -1L
        val contacts = WheelContactTimeline()
    }
    private data class MarkKey(val surface: Long, val block: BlockPos, val axis: Int, val sign: Int, val plane: Double, val residue: Boolean = false)
    private class Pixel(val points: List<Vector3d>, var strength: Float, var time: Long, var color: Vector3f)
    private class Quad(val vertices: FloatArray, val strength: Float, val time: Long, val color: Vector3f)
    private class Mark(val surface: Long, val block: BlockPos, val state: BlockState, val normal: Vector3d, val material: WheelSurface,
                       val texture: BlockState? = null) {
        val pixels = HashMap<Int, Pixel>()
        val bounds = AABB(block).inflate(.003)
        private val flipU = material == WheelSurface.DIRT || material == WheelSurface.GRAVEL
        var geometry: List<Quad>? = null
        private var cachedSprite: TextureAtlasSprite? = null

        fun geometry(sprite: TextureAtlasSprite?): List<Quad> {
            geometry?.takeIf { cachedSprite === sprite }?.let { return it }
            val axis = (0..2).maxBy { abs(normal[it]) }
            val u = (axis + 1) % 3
            val v = (axis + 2) % 3
            val used = BooleanArray(256)
            val result = ArrayList<Quad>()
            fun matches(a: Pixel, b: Pixel) = a.strength == b.strength && a.time == b.time && a.color == b.color
            for (key in 0 until 256) {
                if (used[key]) continue
                val pixel = pixels[key] ?: continue
                val first = pixel.points[0]
                var endU = pixel.points[2][u]
                var endV = pixel.points[2][v]
                var width = 1
                while (key % 16 + width < 16) {
                    val next = pixels[key + width] ?: break
                    if (used[key + width] || !matches(pixel, next) || next.points[0][u] != endU ||
                        next.points[0][v] != first[v] || next.points[2][v] != endV) break
                    endU = next.points[2][u]
                    width++
                }
                var height = 1
                while (key / 16 + height < 16) {
                    val row = key + height * 16
                    val rowEnd = pixels[row]?.points?.get(2)?.get(v) ?: break
                    if ((0 until width).any { offset ->
                        val next = pixels[row + offset]
                        val base = pixels[key + offset]!!
                        next == null || used[row + offset] || !matches(pixel, next) ||
                            next.points[0][u] != base.points[0][u] || next.points[2][u] != base.points[2][u] ||
                            next.points[0][v] != endV || next.points[2][v] != rowEnd
                    }) break
                    endV = rowEnd
                    height++
                }
                for (y in 0 until height) for (x in 0 until width) used[key + y * 16 + x] = true
                val vertices = FloatArray(20)
                for (corner in 0 until 4) {
                    val point = Vector3d(first).setComponent(u, if (corner == 0 || corner == 3) first[u] else endU)
                        .setComponent(v, if (corner < 2) first[v] else endV)
                        .sub(block.x.toDouble(), block.y.toDouble(), block.z.toDouble())
                    val start = corner * 5
                    vertices[start] = point.x.toFloat()
                    vertices[start + 1] = point.y.toFloat()
                    vertices[start + 2] = point.z.toFloat()
                    val texU = if (flipU) 16 - point[u] * 16 else point[u] * 16
                    vertices[start + 3] = sprite?.getU(texU) ?: .5f
                    vertices[start + 4] = sprite?.getV(point[v] * 16) ?: .5f
                }
                result.add(Quad(vertices, pixel.strength, pixel.time, pixel.color))
            }
            cachedSprite = sprite
            geometry = result
            return result
        }
    }
    private data class RimScrape(val tick: Long, val gain: Float, val pitch: Float)
    private val rimScrapes = HashMap<Long, RimScrape>()
    private val wheels = HashMap<Long, Wheel>()
    private val sounds = ArrayList<SurfaceLoop>()
    private val marks = LinkedHashMap<MarkKey, Mark>()
    private val buffers = MultiBufferSource.immediate(BufferBuilder(16384))
    private val renderType = ClockworkRenderTypes.SKID_MARKS
    private val terrainBuffers = MultiBufferSource.immediate(BufferBuilder(16384))
    private var pixelCount = 0
    private var level: ClientLevel? = null
    private var clock = 0L
    private val waterSamples = (-1..1).flatMap { side -> (0 until 8).map { i ->
        Vector3d(side * .7, cos(i * PI / 4) * .94, sin(i * PI / 4) * .94)
    } }

    private fun waterCoverage(world: ClientLevel, body: ClientVsBody): Float {
        val shape = body.collisionShape as? WheelBodyShapeData ?: return 0f
        val bounds = body.renderAABB
        val ships = world.getShipsIntersecting(AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
            bounds.maxX(), bounds.maxY(), bounds.maxZ())).mapNotNull { (it as? ClientShip)?.renderTransform?.worldToShip }
        val pos = BlockPos.MutableBlockPos()
        fun containsWater(point: Vector3d): Boolean {
            pos.set(point.x, point.y, point.z)
            if (!world.hasChunkAt(pos)) return false
            val fluid = world.getFluidState(pos)
            return fluid.`is`(FluidTags.WATER) && point.y < pos.y + fluid.getHeight(world, pos)
        }
        val point = Vector3d()
        val local = Vector3d()
        var wet = 0
        for (sample in waterSamples) {
            point.set(sample.x * shape.halfThickness, sample.y * shape.radius, sample.z * shape.radius)
            body.renderTransform.toWorld.transformPosition(point)
            if (containsWater(point) || ships.any { containsWater(it.transformPosition(point, local)) }) wet++
        }
        return wet.toFloat() / waterSamples.size
    }

    private fun checkLevel() {
        val current = Minecraft.getInstance().level
        if (current === level) return
        sounds.forEach { Minecraft.getInstance().soundManager.stop(it) }
        sounds.clear(); wheels.clear(); marks.clear()
        rimScrapes.clear()
        LayeredTireRenderer.clear()
        pixelCount = 0
        level = current
        clock = 0
    }

    fun accept(packet: WheelContactPacket) {
        checkLevel()
        val world = level ?: return
        if (world.dimension().location() != packet.dimension) return
        val sampleTick = packet.sampleTick ?: world.gameTime
        for (state in packet.wheels) {
            val wheel = wheels.getOrPut(state.body) { Wheel(state, clock) }
            if (!wheel.contacts.add(sampleTick, state.contacts)) continue
            wheel.state = state
            wheel.time = clock
        }
        packet.marks.forEach { stamp(world, it) }
    }

    fun tick() {
        checkLevel()
        val world = level ?: return
        clock++
        wheels.entries.removeIf { clock - it.value.time > 2400 || world.shipObjectWorld.allBodies.getById(it.key) == null }
        surfaceEffects(world)
        marks.entries.removeIf { (_, mark) ->
            val before = mark.pixels.size
            mark.pixels.entries.removeIf { clock - it.value.time >= 2400 }
            if (before != mark.pixels.size) mark.geometry = null
            pixelCount -= before - mark.pixels.size
            mark.pixels.isEmpty()
        }
    }

    private data class Voice(val sound: SoundEvent, val point: Vector3d, var gain: Float, var pitch: Float)

    fun sampleRim(hub: WheelHubBlockEntity, rim: WheelSize) {
        val world = level ?: return
        if (world !== hub.level || (world.gameTime + hub.bodyId) % 4L != 0L) return
        rimScrapes.remove(hub.bodyId)
        val body = world.shipObjectWorld.allBodies.getById(hub.bodyId) as? ClientVsBody ?: return
        val center = body.renderTransform.position
        val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
        if (camera.distanceToSqr(center.x(), center.y(), center.z()) > 20 * 20) return
        val scale = abs(body.renderTransform.scaling.x())
        val radius = rim.rimRadius * scale
        val start = center.toMinecraft()
        val hit = world.clipIncludeShips(ClipContext(start, start.subtract(0.0, radius * 1.5 + rim.rimWidth * scale, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null), skipShip = world.getShipManagingPos(hub.blockPos)?.id)
        if (hit.type != HitResult.Type.BLOCK || hit.isInside) return
        val surface = WheelSurface.scrapeSurface(world.getBlockState(hit.blockPos)) ?: return

        val support = world.getShipManagingPos(hit.blockPos) as? ClientShip
        val face = hit.direction
        val normal = Vector3d(face.stepX.toDouble(), face.stepY.toDouble(), face.stepZ.toDouble())
        support?.renderTransform?.shipToWorldRotation?.transform(normal)
        val axle = body.renderTransform.rotation.transform(Vector3d(1.0, 0.0, 0.0))
        val axial = normal.dot(axle)
        val arm = Vector3d(normal).fma(-axial, axle)
        if (arm.lengthSquared() > 1e-8) arm.normalize(-radius)
        arm.fma(-sign(axial) * rim.rimWidth * scale * .5, axle)
        val point = Vector3d(center).add(arm)
        if (abs(Vector3d(point).sub(hit.location.x, hit.location.y, hit.location.z).dot(normal)) > .045 * scale) return

        val velocity = Vector3d(body.velocity)
        val spin = Vector3d(body.angularVelocity)
        if (support != null) {
            velocity.sub(support.velocity).sub(Vector3d(support.angularVelocity).cross(Vector3d(center).sub(support.renderTransform.positionInWorld)))
            spin.sub(support.angularVelocity)
        }
        val slip = spin.cross(arm).add(velocity)
        slip.fma(-slip.dot(normal), normal)
        velocity.fma(-velocity.dot(normal), normal)
        val strength = ((velocity.length() - .35).coerceIn(0.0, 8.0) * .025 +
            (slip.length() - .25).coerceIn(0.0, 6.0) * .065).toFloat()
        if (strength < .025f) return
        val pitch = (if (surface == WheelSurface.METAL) 1f else .8f) + .18f * strength
        rimScrapes[body.id] = RimScrape(clock, strength, pitch)
    }

    fun coating(body: Long): Coating? = wheels[body]?.coating?.takeIf { it.amount > .01f }

    fun surfaceContact(body: Long): TireSurfaceContact? = wheels[body]?.takeIf { clock - it.time <= 3 }?.state?.effect

    fun coatingSprite(state: BlockState): TextureAtlasSprite {
        val mc = Minecraft.getInstance()
        return if (state.`is`(Blocks.GRASS_BLOCK))
            mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation("minecraft", "block/grass_block_top"))
        else mc.blockRenderer.blockModelShaper.getBlockModel(state).particleIcon
    }

    private fun surfaceEffects(world: ClientLevel) {
        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera.position
        val voices = ArrayList<Voice>()
        var budget = 64
        var sandCloudBudget = 8
        var impactSound = false
        fun voice(sound: SoundEvent, point: Vector3d, gain: Float, pitch: Float) {
            if (gain < .005f) return
            val group = voices.firstOrNull { it.sound == sound && it.point.distanceSquared(point) < 36 }
            if (group == null) voices.add(Voice(sound, Vector3d(point), gain, pitch))
            else {
                group.point.lerp(point, .25)
                if (gain > group.gain) { group.gain = gain; group.pitch = pitch }
            }
        }
        rimScrapes.entries.removeIf { (id, scrape) ->
            val body = world.shipObjectWorld.allBodies.getById(id) as? ClientVsBody
            if (body == null || clock - scrape.tick > 5) return@removeIf true
            voice(ClockworkSounds.RIM_SCRAPE.mainEvent!!, Vector3d(body.renderTransform.position), scrape.gain, scrape.pitch)
            false
        }
        fun particle(options: ParticleOptions, point: Vector3d, velocity: Vector3d): Particle? {
            if (budget <= 0) return null
            budget--
            return mc.particleEngine.createParticle(options, point.x, point.y, point.z, 0.0, 0.0, 0.0)?.also {
                it.setParticleSpeed(velocity.x / 20, velocity.y / 20, velocity.z / 20)
            }
        }
        for ((id, wheel) in wheels) {
            val body = world.shipObjectWorld.allBodies.getById(id) as? ClientVsBody ?: continue
            val type = wheel.state.type
            wheel.coating.amount = (wheel.coating.amount - .0004f).coerceAtLeast(0f)
            val effect = wheel.state.effect?.takeIf { clock - wheel.time <= 4 }
            val water = if (type.floating || wheel.coating.amount > 0f || effect != null) waterCoverage(world, body) else 0f
            if (type.floating && water > .01f && clock - wheel.time <= 4) {
                val shape = body.collisionShape as? WheelBodyShapeData
                if (shape != null) {
                    val speed = (body.angularVelocity.length() * shape.radius).toFloat()
                    val center = body.renderTransform.position
                    voice(ClockworkSounds.DRIVING_WET.mainEvent!!, Vector3d(center),
                        water * (speed / 12).coerceIn(0f, .7f), .75f + (speed / 30).coerceAtMost(.6f))
                    if (speed > .5f && camera.distanceToSqr(center.x(), center.y(), center.z()) < 48 * 48) {
                        val point = body.renderTransform.toWorld.transformPosition(Vector3d(0.0, -shape.radius, 0.0))
                        val pos = BlockPos.containing(point.x, point.y, point.z)
                        if (world.getFluidState(pos).`is`(FluidTags.WATER))
                            particle(ParticleTypes.BUBBLE, point, Vector3d(body.velocity).mul(.15).add(0.0, .3, 0.0))
                    }
                }
            }
            if (water > 0f) {
                wheel.coating.amount = (wheel.coating.amount - water * .025f).coerceAtLeast(0f)
                wheel.coating.trail = (wheel.coating.trail - 1).coerceAtLeast(0)
            }
            if (effect == null) {
                wheel.trackPoint = null
                wheel.trackDistance = 0.0
                wheel.coating.trail = (wheel.coating.trail - 1).coerceAtLeast(0)
                wheel.heat = (wheel.heat - .012f).coerceAtLeast(0f)
                wheel.airborne++
                wheel.dust = 0.0; wheel.smoke = 0.0
                continue
            }
            val surface = if (effect.surface == -1L) null else world.shipObjectWorld.allBodies.getById(effect.surface) as? ClientVsBody
            if (effect.surface != -1L && surface == null) continue
            val point = Vector3d(effect.point)
            val normal = Vector3d(effect.normal)
            val velocity = Vector3d(effect.velocity)
            val scrub = Vector3d(effect.scrub)
            surface?.renderTransform?.toWorld?.let {
                it.transformPosition(point); it.transformDirection(normal)
                it.transformDirection(velocity); it.transformDirection(scrub)
            }
            normal.normalize()
            velocity.fma(-velocity.dot(normal), normal)
            val speed = velocity.length()
            point.fma((clock - wheel.time).coerceAtMost(2) / 20.0, velocity).fma(.025, normal)
            if (camera.distanceToSqr(point.x, point.y, point.z) > 48 * 48) {
                wheel.heat = (wheel.heat - .012f).coerceAtLeast(0f)
                wheel.dust = 0.0; wheel.smoke = 0.0
                wheel.load = effect.load; wheel.airborne = 0; wheel.effectTime = wheel.time
                continue
            }
            if (wheel.candidate != effect.material) { wheel.candidate = effect.material; wheel.candidateTicks = 0 }
            if (++wheel.candidateTicks >= 3) wheel.material = wheel.candidate
            val heating = if (effect.material.rubber) (effect.sliding - 1f).coerceIn(0f, 5f) * .0075f else 0f
            wheel.heat = (wheel.heat + heating - .008f).coerceIn(0f, 1.5f)
            val roll = ((speed - .2) / 10).coerceIn(0.0, 1.0).toFloat()
            val skid = ((effect.sliding - .5f) / 3.5f).coerceIn(0f, 1f)
            voice(type.rollingSound?.let { BuiltInRegistries.SOUND_EVENT.get(it) } ?: rollingSound(wheel.material), point, sqrt(roll) * .3f,
                (.75 + speed * .025).coerceIn(.75, 1.3).toFloat())
            voice(type.skidSound?.let { BuiltInRegistries.SOUND_EVENT.get(it) } ?: skidSound(wheel.material), point, skid * .55f, .85f + skid * .3f)
            val state = Block.stateById(effect.blockState)
            val grassSkid = effect.material == WheelSurface.GRASS && skid > .25f
            val smears = water == 0f && (grassSkid ||
                effect.material == WheelSurface.SAND || effect.material == WheelSurface.SNOW || effect.material.rollingTracks(state))
            val coating = wheel.coating
            val travel = (speed / 20).coerceAtMost(1.0).toFloat()
            if (smears && (travel > .005f || skid > .05f)) {
                val deposit = if (grassSkid) Blocks.GRASS_BLOCK.defaultBlockState() else state
                if (coating.state != deposit && coating.amount > .05f) {
                    coating.amount = (coating.amount - travel * .05f - skid * .01f - .002f).coerceAtLeast(0f)
                } else {
                    coating.state = deposit
                    val source = BlockPos.containing(effect.point.x, effect.point.y - .02, effect.point.z)
                    val color = if (grassSkid) mc.blockColors.getColor(deposit, world, source, 0) else 0xFFFFFF
                    coating.color.set(((color shr 16) and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
                    coating.amount = (coating.amount + travel * .018f + skid * .006f).coerceAtMost(1f)
                }
                coating.trail = 80
            } else {
                coating.amount = (coating.amount - travel * .012f).coerceAtLeast(0f)
                coating.trail = (coating.trail - 1).coerceAtLeast(0)
            }
            val trackPoint = Vector3d(effect.point).fma((clock - wheel.time).coerceAtMost(2) / 20.0, effect.velocity)
            val previous = wheel.trackPoint
            if (previous != null && wheel.trackSurface == effect.surface) {
                val delta = Vector3d(trackPoint).sub(previous)
                val distance = delta.length()
                if (distance in .015..1.5) {
                    if (!smears && water == 0f && coating.trail > 0 && coating.amount > .03f)
                        stamp(world, TireMark(effect.surface, Vector3d(trackPoint).add(previous).mul(.5),
                            effect.normal, delta.div(distance), effect.width * .84f, (distance + .04).toFloat(), 0f, 1f),
                            coating, wheel.trackDistance + distance * .5)
                    wheel.trackDistance = (wheel.trackDistance + distance) % 16.0
                } else if (distance > 1.5) wheel.trackDistance = 0.0
                if (distance >= .015) wheel.trackPoint = trackPoint
            } else {
                wheel.trackPoint = trackPoint
                wheel.trackDistance = 0.0
            }
            wheel.trackSurface = effect.surface
            val sourcePos = BlockPos.containing(effect.point.x - effect.normal.x * .02,
                effect.point.y - effect.normal.y * .02, effect.point.z - effect.normal.z * .02)
            val rgb = state.getMapColor(world, sourcePos).col
            val tint = Vector3f(((rgb shr 16) and 255) / 255f, ((rgb shr 8) and 255) / 255f, (rgb and 255) / 255f)
                .lerp(Vector3f(.8f), .15f)
            fun debris(impact: Boolean = false) {
                val random = world.random
                val spread = Vector3d(random.nextDouble() - .5, random.nextDouble() - .5, random.nextDouble() - .5)
                spread.fma(-spread.dot(normal), normal).mul(effect.width.toDouble())
                val origin = Vector3d(point).add(spread)
                val kick = Vector3d(effect.surfaceVelocity).fma(.15, scrub).fma(-.08, velocity)
                    .fma((if (impact) 1.5 else .6) + random.nextDouble(), normal)
                    .add((random.nextDouble() - .5) * .8, 0.0, (random.nextDouble() - .5) * .8)
                val options: ParticleOptions = when {
                    effect.material == WheelSurface.WET -> ParticleTypes.SPLASH
                    effect.material == WheelSurface.SNOW -> ParticleTypes.SNOWFLAKE
                    effect.material == WheelSurface.GRAVEL || effect.material == WheelSurface.GRASS || random.nextInt(4) == 0 ->
                        BlockParticleOption(ParticleTypes.BLOCK, state)
                    else -> DustParticleOptions(tint, if (impact) 1f else .7f)
                }
                particle(options, origin, kick)
                val sandDrift = ((skid - .2f) / .8f).coerceIn(0f, 1f)
                if (!impact && effect.material == WheelSurface.SAND && water == 0f && sandCloudBudget > 0 &&
                    random.nextFloat() < sandDrift) {
                    val drift = Vector3d(effect.surfaceVelocity)
                        .fma(minOf(.5, 5.0 / maxOf(1.0, scrub.length())), scrub)
                        .fma(-minOf(.2, 3.0 / maxOf(1.0, speed)), velocity)
                        .fma(1.2 + sandDrift + random.nextDouble(), normal)
                        .add(Vector3d(spread).mul(2.0))
                    val cloudPoint = Vector3d(origin).fma(.2, normal).fma(-.025, velocity)
                    particle(ParticleTypes.CLOUD, cloudPoint, drift)?.apply {
                        val shade = .85f + random.nextFloat() * .15f
                        setColor(tint.x * shade, tint.y * shade, tint.z * shade)
                        scale((2.5f + sandDrift * 2f) * (.8f + random.nextFloat() * .4f))
                        setLifetime(30 + (sandDrift * 20).toInt() + random.nextInt(10))
                    }
                    sandCloudBudget--
                }
            }
            if (effect.material.loose) {
                val density = when (effect.material) { WheelSurface.GRASS -> .25; WheelSurface.GRAVEL -> .6; else -> 1.0 }
                wheel.dust = (wheel.dust + (speed * .035 + skid * .65).coerceAtMost(1.5) * density).coerceAtMost(3.0)
                while (wheel.dust >= 1 && budget > 0) { debris(); wheel.dust-- }
            } else wheel.dust = 0.0
            if (type.skidSmoke && effect.material.rubber && wheel.heat > .35f) {
                wheel.smoke = (wheel.smoke + (wheel.heat - .35) * .6).coerceAtMost(2.0)
                while (wheel.smoke >= 1 && budget > 0) {
                    val drift = Vector3d(effect.surfaceVelocity).fma(.1, velocity).fma(.5, normal).add(0.0, .4, 0.0)
                    particle(ParticleTypes.SMOKE, point, drift)
                    wheel.smoke--
                }
            } else wheel.smoke = 0.0
            if (wheel.effectTime != wheel.time) {
                var impact = if (wheel.load > 0) ((effect.peak - wheel.load * 1.8f) / maxOf(5000f, wheel.load * 3)).coerceIn(0f, 1f) else 0f
                if (wheel.airborne > 2) impact = maxOf(impact, (effect.peak / maxOf(5000f, wheel.load) * .3f).coerceIn(0f, 1f))
                if (impact > .12f && clock - wheel.lastImpact >= 10) {
                    repeat(2 + (impact * 6).toInt()) { debris(true) }
                    if (!impactSound) {
                        val sound = type.impactSound?.let { BuiltInRegistries.SOUND_EVENT.get(it) } ?: state.soundType.fallSound
                        world.playLocalSound(point.x, point.y, point.z, sound,
                            SoundSource.BLOCKS, .2f + impact * .5f, .8f + world.random.nextFloat() * .2f, false)
                        impactSound = true
                    }
                    wheel.lastImpact = clock
                }
                wheel.load = if (wheel.load == 0f) effect.load else wheel.load * .7f + effect.load * .3f
                wheel.airborne = 0
                wheel.effectTime = wheel.time
            }
        }
        sounds.removeIf { it.isStopped }
        val available = sounds.toMutableList()
        sounds.forEach { it.target = 0f }
        for (voice in voices.sortedBy { camera.distanceToSqr(it.point.x, it.point.y, it.point.z) }.take(12)) {
            var sound = available.filter { it.event == voice.sound && it.position.distanceSquared(voice.point) < 144 }
                .minByOrNull { it.position.distanceSquared(voice.point) }
            if (sound == null) {
                if (sounds.size >= 16) continue
                sound = SurfaceLoop(voice.sound, world, Vector3d(voice.point))
                sounds.add(sound)
                mc.soundManager.play(sound)
            } else available.remove(sound)
            sound.position.set(voice.point)
            sound.target = voice.gain
            sound.targetPitch = voice.pitch
        }
    }

    private fun rollingSound(surface: WheelSurface): SoundEvent = when (surface) {
        WheelSurface.GRASS, WheelSurface.DIRT, WheelSurface.SAND, WheelSurface.GRAVEL -> ClockworkSounds.DRIVING_GRITTY
        WheelSurface.WOOD -> ClockworkSounds.DRIVING_WOOD
        WheelSurface.ICE -> ClockworkSounds.DRIVING_ICY
        WheelSurface.METAL -> ClockworkSounds.DRIVING_METAL
        WheelSurface.SNOW -> ClockworkSounds.DRIVING_SNOW
        WheelSurface.WET -> ClockworkSounds.DRIVING_WET
        else -> ClockworkSounds.DRIVING_GENERIC
    }.mainEvent!!

    private fun skidSound(surface: WheelSurface): SoundEvent = when (surface) {
        WheelSurface.GRASS -> ClockworkSounds.TIRE_SKID_GRASSY
        WheelSurface.DIRT, WheelSurface.SAND, WheelSurface.GRAVEL -> ClockworkSounds.TIRE_SKID_GRITTY
        WheelSurface.ICE -> ClockworkSounds.TIRE_SKID_ICY
        WheelSurface.METAL -> ClockworkSounds.TIRE_SKID_METAL
        WheelSurface.SNOW -> ClockworkSounds.TIRE_SKID_SNOW
        WheelSurface.WET -> ClockworkSounds.TIRE_SKID_WET
        else -> ClockworkSounds.TIRE_SQUEAL
    }.mainEvent!!

    fun compression(body: ClientVsBody, partialTick: Float): List<TireContact> {
        checkLevel()
        val wheel = wheels[body.id] ?: return emptyList()
        val world = level ?: return emptyList()
        val playback = (world.gameTime - CONTACT_INTERVAL).toDouble() + partialTick
        return wheel.contacts.sample(playback).mapNotNull { contact ->
            val normal = Vector3d(contact.normal)
            if (contact.surface != -1L) {
                val transform = (world.shipObjectWorld.allShips.getById(contact.surface) as? ClientShip)?.renderTransform
                    ?: (world.shipObjectWorld.allBodies.getById(contact.surface) as? ClientVsBody)?.renderTransform
                    ?: return@mapNotNull null
                transform.toWorld.transformDirection(normal)
            }
            body.renderTransform.toModel.transformDirection(normal).normalize()
            TireContact(contact.surface, normal, (contact.compression / body.renderTransform.scaling.x()).toFloat())
        }
    }

    private fun stamp(world: ClientLevel, mark: TireMark, residue: Coating? = null, trailDistance: Double = 0.0) {
        if (mark.normal.lengthSquared() < .5 || mark.forward.lengthSquared() < .5 || mark.width <= 0 || mark.length <= 0) return
        val amount = ((mark.strength - .1f) / .9f).coerceIn(0f, 1f)
        val skid = amount * amount * (3 - 2 * amount)
        if (skid < .01f && mark.rolling <= .01f) return
        val normal = Vector3d(mark.normal).normalize()
        val axis = (0..2).maxBy { abs(normal[it]) }
        if (abs(normal[axis]) < .92) return
        val sign = if (normal[axis] > 0) 1 else -1
        val u = (axis + 1) % 3
        val v = (axis + 2) % 3
        normal.zero().setComponent(axis, sign.toDouble())
        val forward = Vector3d(mark.forward).setComponent(axis, 0.0)
        if (forward.lengthSquared() < .01) return
        forward.normalize().mul(mark.length * .5)
        val side = Vector3d(normal).cross(forward).normalize().mul(mark.width * .5)
        val corners = listOf(Vector3d(mark.center).sub(forward).sub(side), Vector3d(mark.center).add(forward).sub(side),
            Vector3d(mark.center).add(forward).add(side), Vector3d(mark.center).sub(forward).add(side))
        val min = Vector3d(corners[0]); val max = Vector3d(min)
        corners.forEach { min.min(it); max.max(it) }
        min.sub(.06, .06, .06); max.add(.06, .06, .06)
        val from = BlockPos.containing(min.x, min.y, min.z)
        val to = BlockPos.containing(max.x, max.y, max.z)
        if ((to.x - from.x + 1) * (to.y - from.y + 1) * (to.z - from.z + 1) > 64) return
        for (pos in BlockPos.betweenClosed(from, to)) {
            if (!world.hasChunkAt(pos)) continue
            val state = world.getBlockState(pos)
            val material = WheelSurface.of(state)
            if (material == WheelSurface.ICE || (material == WheelSurface.WET && !material.rollingTracks(state))) continue
            val strength = residue?.amount ?: if (material.rollingTracks(state)) maxOf(skid, mark.rolling * .32f) else skid
            if (strength < .01f) continue
            val raisedSurface = axis == 1 && sign > 0 && (state.block is SnowLayerBlock || state.block is MudBlock)
            val shape = if (raisedSurface) state.getShape(world, pos) else state.getCollisionShape(world, pos)
            for (box in shape.toAabbs()) {
                val low = doubleArrayOf(box.minX + pos.x, box.minY + pos.y, box.minZ + pos.z)
                val high = doubleArrayOf(box.maxX + pos.x, box.maxY + pos.y, box.maxZ + pos.z)
                val plane = if (sign > 0) high[axis] else low[axis]
                val contactPlane = plane - if (raisedSurface) .125 else 0.0
                if (abs(contactPlane - mark.center[axis]) > .06 &&
                    (!raisedSurface || abs(plane - mark.center[axis]) > .06)) continue
                var polygon = corners
                for (a in intArrayOf(u, v)) {
                    polygon = clip(polygon, a, low[a], true)
                    polygon = clip(polygon, a, high[a], false)
                }
                if (polygon.size < 3) continue
                val block = pos.immutable()
                val origin = doubleArrayOf(block.x.toDouble(), block.y.toDouble(), block.z.toDouble())
                val minU = floor((polygon.minOf { it[u] } - origin[u]) * 16).toInt().coerceIn(0, 15)
                val maxU = floor((polygon.maxOf { it[u] } - origin[u]) * 16).toInt().coerceIn(0, 15)
                val minV = floor((polygon.minOf { it[v] } - origin[v]) * 16).toInt().coerceIn(0, 15)
                val maxV = floor((polygon.maxOf { it[v] } - origin[v]) * 16).toInt().coerceIn(0, 15)
                val key = MarkKey(mark.surface, block, axis, sign, plane, residue != null)
                marks[key]?.takeIf { it.state != state || it.texture != residue?.state }
                    ?.let { pixelCount -= it.pixels.size; marks.remove(key) }
                for (x in minU..maxU) for (y in minV..maxV) {
                    val u0 = maxOf(low[u], origin[u] + x / 16.0)
                    val u1 = minOf(high[u], origin[u] + (x + 1) / 16.0)
                    val v0 = maxOf(low[v], origin[v] + y / 16.0)
                    val v1 = minOf(high[v], origin[v] + (y + 1) / 16.0)
                    if (u1 <= u0 || v1 <= v0) continue
                    var covered = clip(clip(polygon, u, u0, true), u, u1, false)
                    covered = clip(clip(covered, v, v0, true), v, v1, false)
                    if (covered.size < 3) continue
                    val base = covered[0]
                    val area = (1 until covered.lastIndex).sumOf { i ->
                        (covered[i][u] - base[u]) * (covered[i + 1][v] - base[v]) -
                            (covered[i][v] - base[v]) * (covered[i + 1][u] - base[u])
                    }
                    if (abs(area) < 1e-10) continue
                    val pixelKey = x + y * 16
                    val seed = (block.asLong() xor (pixelKey.toLong() * 0x9E3779B9L)).hashCode()
                    val grain = (seed.ushr(8) and 255) / 255f
                    val center = Vector3d().setComponent(axis, plane).setComponent(u, (u0 + u1) / 2).setComponent(v, (v0 + v1) / 2)
                    val lateral = abs(Vector3d(center).sub(mark.center).dot(side)) / side.lengthSquared()
                    val edge = lateral > .72
                    val tread = Math.floorMod(floor(center.dot(forward) / forward.length() * 16 / 3).toInt(), 2) == 0
                    val color: Vector3f
                    val opacity: Float
                    if (residue != null) {
                        if (lateral > .85 && grain > .7f) continue
                        val along = trailDistance + Vector3d(center).sub(mark.center).dot(forward) / forward.length()
                        val groove = Math.floorMod(floor(along * 16).toInt(), 8) < 2
                        val fade = residue.trail / 80f
                        color = Vector3f(residue.color).mul(.82f + grain * .15f)
                        opacity = sqrt(strength) * fade * .65f * (if (groove) .65f else 1f) * (if (edge) .7f else 1f)
                    } else when (material) {
                        WheelSurface.GRASS -> {
                            if (grain > (.2f + .7f * skid) * (if (edge) .6f else 1f)) continue
                            color = Vector3f(.85f + grain * .15f)
                            opacity = skid * (.45f + .5f * skid)
                        }
                        WheelSurface.DIRT, WheelSurface.GRAVEL -> {
                            if (grain > .7f) continue
                            color = Vector3f(.6f + grain * .25f)
                            opacity = strength * .25f
                        }
                        WheelSurface.SAND -> {
                            color = Vector3f(if (edge) 1f else if (tread) .64f else .84f)
                            opacity = strength * .75f
                        }
                        WheelSurface.SNOW -> {
                            color = if (edge) Vector3f(1f) else if (tread) Vector3f(.64f, .72f, .84f) else Vector3f(.83f, .87f, .94f)
                            opacity = strength * .85f
                        }
                        WheelSurface.WET -> {
                            color = Vector3f(if (edge) .95f else if (tread) .48f else .72f)
                            opacity = strength * .75f
                        }
                        else -> { color = Vector3f(.055f, .05f, .045f); opacity = strength * .42f }
                    }
                    val face = marks.getOrPut(key) { Mark(mark.surface, block, state, normal, material, residue?.state) }
                    val old = face.pixels[pixelKey]
                    if (old == null) {
                        val points = listOf(u0 to v0, u1 to v0, u1 to v1, u0 to v1).map { (a, b) ->
                            Vector3d().setComponent(axis, plane + sign * .002).setComponent(u, a).setComponent(v, b)
                        }
                        face.pixels[pixelKey] = Pixel(points, opacity, clock, color)
                        face.geometry = null
                        pixelCount++
                    } else if (opacity >= old.strength * fade(clock - old.time)) {
                        old.strength = opacity
                        old.color = color
                        old.time = clock
                        face.geometry = null
                    }
                }
                while (pixelCount > 32768) pixelCount -= marks.remove(marks.keys.first())!!.pixels.size
            }
        }
    }

    private fun fade(age: Long, partialTick: Float = 0f) = ((2400 - age - partialTick) / 400f).coerceIn(0f, 1f)

    private fun clip(points: List<Vector3d>, axis: Int, boundary: Double, above: Boolean): List<Vector3d> {
        if (points.isEmpty()) return points
        val output = ArrayList<Vector3d>()
        var previous = points.last()
        var inside = (previous[axis] >= boundary) == above
        for (point in points) {
            val nextInside = (point[axis] >= boundary) == above
            if (inside != nextInside) output.add(Vector3d(previous).lerp(point, (boundary - previous[axis]) / (point[axis] - previous[axis])))
            if (nextInside) output.add(point)
            previous = point; inside = nextInside
        }
        return output
    }

    @JvmStatic
    fun renderSkidmarks(stack: PoseStack, camera: Camera, partialTick: Float, projection: Matrix4f) {
        checkLevel()
        val world = level ?: return
        if (marks.isEmpty() || ShaderPackCompat.shadowPass()) return
        val rubberBuffer = buffers.getBuffer(renderType)
        val terrainBuffer = terrainBuffers.getBuffer(ClockworkRenderTypes.TERRAIN_TRACKS)
        val cameraPosition = camera.position
        val frustum = Frustum(stack.last().pose(), projection).also {
            it.prepare(cameraPosition.x, cameraPosition.y, cameraPosition.z)
        }
        val point = Vector3f()
        val normal = Vector3f()
        val center = Vector3d()
        val bounds = AABBd()
        val iterator = marks.values.iterator()
        while (iterator.hasNext()) {
            val mark = iterator.next()
            val body = if (mark.surface == -1L) null else world.shipObjectWorld.allBodies.getById(mark.surface) as? ClientVsBody
            if (mark.surface != -1L && body == null) { pixelCount -= mark.pixels.size; iterator.remove(); continue }
            center.set(mark.block.x + .5, mark.block.y + .5, mark.block.z + .5)
            body?.renderTransform?.toWorld?.transformPosition(center)
            if (cameraPosition.distanceToSqr(center.x, center.y, center.z) > 96 * 96) continue
            val visibleBounds = if (body == null) mark.bounds else {
                bounds.setMin(mark.bounds.minX, mark.bounds.minY, mark.bounds.minZ)
                    .setMax(mark.bounds.maxX, mark.bounds.maxY, mark.bounds.maxZ)
                    .transform(body.renderTransform.toWorld).toMinecraft()
            }
            if (!frustum.isVisible(visibleBounds)) continue
            if (!world.hasChunkAt(mark.block)) continue
            if (world.getBlockState(mark.block) != mark.state) { pixelCount -= mark.pixels.size; iterator.remove(); continue }
            stack.pushPose()
            val relative = Matrix4d().translation(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z)
            if (body != null) {
                relative.mul(body.renderTransform.toWorld)
                stack.last().normal().rotate(Quaternionf(body.renderTransform.rotation))
            }
            relative.translate(mark.block.x.toDouble(), mark.block.y.toDouble(), mark.block.z.toDouble())
            stack.mulPoseMatrix(Matrix4f(relative))
            val pose = stack.last()
            val light = LevelRenderer.getLightColor(world, mark.block.relative(Direction.getNearest(mark.normal.x, mark.normal.y, mark.normal.z)))
            val rubber = mark.material.rubber && mark.texture == null
            val buffer = if (rubber) rubberBuffer else terrainBuffer
            val textureState = mark.texture ?: if (mark.material == WheelSurface.GRASS) Blocks.DIRT.defaultBlockState() else mark.state
            val sprite = if (rubber) null else coatingSprite(textureState)
            pose.normal().transform(normal.set(mark.normal.x.toFloat(), mark.normal.y.toFloat(), mark.normal.z.toFloat()))
            for (quad in mark.geometry(sprite)) {
                val alpha = quad.strength * fade(clock - quad.time, partialTick)
                val vertices = quad.vertices
                for (i in 0 until 4) {
                    val start = i * 5
                    pose.pose().transformPosition(vertices[start], vertices[start + 1], vertices[start + 2], point)
                    buffer.vertex(point.x, point.y, point.z, quad.color.x, quad.color.y, quad.color.z, alpha,
                        vertices[start + 3], vertices[start + 4], OverlayTexture.NO_OVERLAY, light,
                        normal.x, normal.y, normal.z)
                }
            }
            stack.popPose()
        }
        buffers.endBatch()
        terrainBuffers.endBatch()
    }

    private class SurfaceLoop(val event: SoundEvent, val world: ClientLevel, val position: Vector3d) :
        AbstractTickableSoundInstance(event, SoundSource.BLOCKS, RandomSource.create()) {
        var target = 0f
        var targetPitch = 1f
        init { looping = true; delay = 0; volume = 0f; x = position.x; y = position.y; z = position.z }
        override fun canStartSilent() = true
        override fun tick() {
            if (Minecraft.getInstance().level !== world) { stop(); return }
            x = position.x; y = position.y; z = position.z
            volume += (target - volume) * .25f
            pitch += (targetPitch - pitch) * .2f
            if (target == 0f && volume < .003f) stop()
        }
    }
}
