package org.valkyrienskies.clockwork.effekseer.api.common

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.jetbrains.annotations.ApiStatus
import java.lang.ref.WeakReference
import java.util.function.Consumer
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.platform.NativePlatform
import org.valkyrienskies.clockwork.effekseer.client.registry.EffectRegistry
import org.valkyrienskies.clockwork.effekseer.common.network.AddParticlePacket
import org.valkyrienskies.clockwork.effekseer.common.util.Basis
import java.util.*
import kotlin.collections.ArrayList


open class ParticleEmitterInfo : Cloneable {
    /**
     * Create a packet when on logic server,
     * with an anonymous emitter that can't be referenced later.
     */
    fun create(level: Level, location: ResourceLocation?): ParticleEmitterInfo {
        return if (level.isClientSide()
        ) ParticleEmitterInfo(location)
        else AddParticlePacket(location)
    }

    /**
     * Create a packet when on logic server,
     * with a named emitter that can be referenced later.
     */
    fun create(level: Level, location: ResourceLocation?, emitterName: ResourceLocation?): ParticleEmitterInfo {
        return if (level.isClientSide()
        ) ParticleEmitterInfo(location, emitterName)
        else AddParticlePacket(location, emitterName)
    }

    var effek: ResourceLocation? = null
    var emitter: ResourceLocation? = null
    protected var flags: Int = 0
    protected var x: Double = 0.0
    protected var y: Double = 0.0
    protected var z: Double = 0.0
    protected var rotX: Float = 0f
    protected var rotY: Float = 0f
    protected var rotZ: Float = 0f
    protected var scaleX: Float = 1f
    protected var scaleY: Float = 1f
    protected var scaleZ: Float = 1f
    protected var esX: Double = 0.0
    protected var esY: Double = 0.0
    protected var esZ: Double = 0.0
    protected var boundEntity: Int = 0
    protected val parameters: MutableList<DynamicParameter> = ArrayList<DynamicParameter>()
    protected val triggers: IntList = IntArrayList()


    /**
     * @see .create
     */
    @ApiStatus.Internal
    constructor(effek: ResourceLocation?) {
        ParticleEmitterInfo(effek, null)
    }

    /**
     * @see .create
     */
    @ApiStatus.Internal
    constructor(effek: ResourceLocation?, emitter: ResourceLocation?) {
        this.effek = effek
        this.emitter = emitter
        if (emitter != null) {
            flags = flags or 1
        }
    }

    override fun clone(): ParticleEmitterInfo {
        try {
            return super.clone() as ParticleEmitterInfo
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }
    }

    fun hasEmitter(): Boolean {
        return (flags and 1) != 0
    }

    fun isPositionSet(): Boolean {
        return (flags and 2) != 0
    }

    fun isRotationSet(): Boolean {
        return (flags and 4) != 0
    }

    fun isScaleSet(): Boolean {
        return (flags and 8) != 0
    }

    fun hasParameters(): Boolean {
        return (flags and 128) != 0
    }

    fun hasTriggers(): Boolean {
        return (flags and 256) != 0
    }

    fun hasBoundEntity(): Boolean {
        return (flags and 16) != 0
    }

    /**
     * Set whether position and rotation are in entity space.
     * @return True if coordinates are in entity space, otherwise in world space.
     */
    fun isEntitySpaceRelativePosSet(): Boolean {
        return (flags and 32) != 0
    }

    fun usingEntityHeadSpace(): Boolean {
        return (flags and 64) != 0
    }

    /**
     * Set position. <br></br>
     * Will be relative position (in world space) if [.hasBoundEntity]
     * @param pos Relative/Absolute position
     * @return self
     * @see .entitySpaceRelativePosition
     */
    fun position(pos: Vec3): ParticleEmitterInfo {
        return position(pos.x, pos.y, pos.z)
    }

    /**
     * Set position. <br></br>
     * Will be relative position if [.hasBoundEntity]
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @return self
     */
    fun position(x: Double, y: Double, z: Double): ParticleEmitterInfo {
        this.x = x
        this.y = y
        this.z = z
        flags = flags or 2
        return this
    }

    /**
     * Set rotation in radians.<br></br>
     * Euler order is YXZ
     * @param rot rotation vector(x, y), in radians
     * @return self
     */
    fun rotation(rot: Vec2): ParticleEmitterInfo {
        return rotation(rot.x, rot.y, 0f)
    }

    /**
     * Set rotation in radians.<br></br>
     * Euler order is YXZ
     * @param x X rotation, in radians
     * @param y Y rotation, in radians
     * @param z Z rotation, in radians
     * @return self
     */
    fun rotation(x: Float, y: Float, z: Float): ParticleEmitterInfo {
        this.rotX = x
        this.rotY = y
        this.rotZ = z
        flags = flags or 4
        return this
    }

    fun scale(scale: Float): ParticleEmitterInfo {
        return scale(scale, scale, scale)
    }

    fun scale(x: Float, y: Float, z: Float): ParticleEmitterInfo {
        this.scaleX = x
        this.scaleY = y
        this.scaleZ = z
        flags = flags or 8
        return this
    }

    fun parameter(index: Int, value: Float): ParticleEmitterInfo {
        parameters.add(DynamicParameter(index, value))
        flags = flags or 128
        return this
    }

    fun trigger(index: Int): ParticleEmitterInfo {
        triggers.add(index)
        flags = flags or 256
        return this
    }

    fun bindOnEntity(entity: Entity): ParticleEmitterInfo {
        this.boundEntity = entity.getId()
        flags = flags or 16
        return this
    }

    /**
     * Set relative position in entity space. <br></br>
     * Will be relative position (in world space) if [.hasBoundEntity]
     * @param pos Relative position in entity space
     * @return self
     */
    fun entitySpaceRelativePosition(pos: Vec3): ParticleEmitterInfo {
        return entitySpaceRelativePosition(pos.x, pos.y, pos.z)
    }

    /**
     * Set relative position in entity space. <br></br>
     * Will be relative position (in world space) if [.hasBoundEntity]
     * @param x Relative X position in entity space
     * @param y Relative Y position in entity space
     * @param z Relative Z position in entity space
     * @return self
     */
    fun entitySpaceRelativePosition(x: Double, y: Double, z: Double): ParticleEmitterInfo {
        this.esX = x
        this.esY = y
        this.esZ = z
        flags = flags or 32
        return this
    }

    fun useEntityHeadSpace(): ParticleEmitterInfo {
        return useEntityHeadSpace(true)
    }

    fun useEntityHeadSpace(value: Boolean): ParticleEmitterInfo {
        flags = if (value) {
            flags or 64
        } else {
            flags and 64.inv()
        }
        return this
    }

    fun position(): Vec3 {
        return if (isPositionSet()) Vec3(x, y, z) else Vec3.ZERO
    }

    fun rotation(): Vec3 {
        return if (isRotationSet()) Vec3(rotX.toDouble(), rotY.toDouble(), rotZ.toDouble()) else Vec3.ZERO
    }

    fun scale(): Vec3 {
        return if (isScaleSet()) Vec3(scaleX.toDouble(), scaleY.toDouble(), scaleZ.toDouble()) else VEC3_ONES
    }

    fun getBoundEntity(level: Level): Optional<Entity> {
        return if (hasBoundEntity()) Optional.ofNullable(level.getEntity(boundEntity)) else Optional.empty()
    }

    fun encode(buf: FriendlyByteBuf) {
        buf.writeResourceLocation(effek)
        buf.writeVarInt(flags)
        if (hasEmitter()) {
            buf.writeResourceLocation(emitter)
        }
        if (isPositionSet()) {
            buf.writeDouble(x)
            buf.writeDouble(y)
            buf.writeDouble(z)
        }
        if (isRotationSet()) {
            buf.writeFloat(rotX)
            buf.writeFloat(rotY)
            buf.writeFloat(rotZ)
        }
        if (isScaleSet()) {
            buf.writeFloat(scaleX)
            buf.writeFloat(scaleY)
            buf.writeFloat(scaleZ)
        }
        if (hasParameters()) {
            buf.writeVarInt(parameters.size)
            parameters.forEach(Consumer<DynamicParameter> { param: DynamicParameter ->
                buf.writeVarInt(param.index)
                buf.writeFloat(param.value)
            })
        }
        if (hasTriggers()) {
            buf.writeVarIntArray(triggers.toIntArray())
        }
        if (hasBoundEntity()) {
            buf.writeVarInt(boundEntity)
        }
        if (isEntitySpaceRelativePosSet()) {
            buf.writeDouble(esX)
            buf.writeDouble(esY)
            buf.writeDouble(esZ)
        }
    }

    constructor(buf: FriendlyByteBuf) {
        effek = buf.readResourceLocation()
        flags = buf.readVarInt()
        emitter = if (hasEmitter()) {
            buf.readResourceLocation()
        } else {
            null
        }
        if (isPositionSet()) {
            x = buf.readDouble()
            y = buf.readDouble()
            z = buf.readDouble()
        }
        if (isRotationSet()) {
            rotX = buf.readFloat()
            rotY = buf.readFloat()
            rotZ = buf.readFloat()
        }
        if (isScaleSet()) {
            scaleX = buf.readFloat()
            scaleY = buf.readFloat()
            scaleZ = buf.readFloat()
        }
        if (hasParameters()) {
            val paramCount = buf.readVarInt()
            for (i in 0 until paramCount) {
                val index = buf.readVarInt()
                val value = buf.readFloat()
                parameters.add(DynamicParameter(index, value))
            }
        }
        if (hasTriggers()) {
            triggers.addElements(0, buf.readVarIntArray())
        }
        if (hasBoundEntity()) {
            boundEntity = buf.readVarInt()
        }
        if (isEntitySpaceRelativePosSet()) {
            esX = buf.readDouble()
            esY = buf.readDouble()
            esZ = buf.readDouble()
        }
    }

    @ApiStatus.Internal
    fun spawnInWorld(level: Level, player: Player?) {
        if (NativePlatform.isRunningOnUnsupportedPlatform()) {
            return
        }
        Optional.ofNullable(EffectRegistry.get(effek)).ifPresent { effek ->
            val emitter = if (hasEmitter()) effek.play(this.emitter) else effek.play()
            val hasBoundEntity = hasBoundEntity()
            val isPositionSet = isPositionSet()
            val isRotationSet = isRotationSet()
            val isScaleSet = isScaleSet()
            val hasParams = hasParameters()
            val hasTriggs = hasTriggers()
            val x: Float
            val y: Float
            val z: Float
            if (isPositionSet) {
                x = this.x.toFloat()
                y = this.y.toFloat()
                z = this.z.toFloat()
            } else if (!hasBoundEntity && player != null) {
                x = player.x.toFloat()
                y = player.y.toFloat()
                z = player.z.toFloat()
            } else {
                z = 0f
                y = z
                x = y
            }
            emitter.setPosition(x, y, z)

            if (isRotationSet) {
                emitter.setRotation(rotX, rotY, rotZ)
            }
            if (isScaleSet) {
                emitter.setScale(scaleX, scaleY, scaleZ)
            }

            if (hasParams) {
                for (parameter in parameters) {
                    emitter.setDynamicInput(parameter.index, parameter.value)
                }
            }
            if (hasTriggs) {
                triggers.forEach(emitter::sendTrigger)
            }
            if (hasBoundEntity) {
                val entity = WeakReference(level.getEntity(boundEntity))
                val headSpace = usingEntityHeadSpace()
                val entitySpace = headSpace || isEntitySpaceRelativePosSet()
                val rotZ: Float = this.rotZ
                emitter.addPreDrawCallback { em, partial ->
                    Optional.ofNullable(entity.get()).filter(Entity::isAlive).ifPresentOrElse({ et ->
                        val relX: Float
                        val relY: Float
                        val relZ: Float
                        if (entitySpace) {
                            val basis: Basis
                            val rotY: Float
                            val rotX: Float
                            if (headSpace) {
                                rotY = Math.toRadians(et.getViewYRot(partial).toDouble()).toFloat()
                                rotX = Math.toRadians(et.getViewXRot(partial).toDouble()).toFloat()
                                basis = Basis.fromEuler(
                                    Vec3(
                                        rotX.toDouble(),
                                        (Mth.PI - rotY).toDouble(),
                                        rotZ.toDouble()
                                    )
                                )
                            } else {
                                rotY =
                                    Math.toRadians(Mth.lerp(partial, et.yRotO, et.getYRot()).toDouble())
                                        .toFloat()
                                rotX = 0f
                                basis = Basis.fromEntityBody(et)
                            }
                            val esRelPos = basis.toGlobal(Vec3(esX, esY, esZ))
                            relX = (x + esRelPos.x).toFloat()
                            relY = (y + esRelPos.y).toFloat()
                            relZ = (z + esRelPos.z).toFloat()
                            em.setRotation(this.rotX + rotX, this.rotY - rotY, rotZ)
                        } else {
                            relX = x
                            relY = y
                            relZ = z
                        }
                        em.setPosition(
                            (Mth.lerp(partial.toDouble(), et.xOld, et.getX()).toFloat() + relX),
                            Mth.lerp(partial.toDouble(), et.yOld, et.getY())
                                .toFloat() + relY + (if (headSpace) et.eyeHeight else 0.0f),
                            Mth.lerp(partial.toDouble(), et.zOld, et.getZ()).toFloat() + relZ
                        )
                    }, em::stop)
                }
            }
        }
    }

    @ApiStatus.Internal
    fun copyTo(target: ParticleEmitterInfo) {
        target.flags = this.flags
        target.x = this.x
        target.y = this.y
        target.z = this.z
        target.rotX = this.rotX
        target.rotY = this.rotY
        target.rotZ = this.rotZ
        target.scaleX = this.scaleX
        target.scaleY = this.scaleY
        target.scaleZ = this.scaleZ
        target.parameters.clear()
        target.parameters.addAll(this.parameters)
        target.triggers.clear()
        target.triggers.addAll(this.triggers)
        target.boundEntity = this.boundEntity
    }

    companion object {
        @JvmStatic
        private val VEC3_ONES: Vec3 = Vec3(1.0, 1.0, 1.0)
    }
}