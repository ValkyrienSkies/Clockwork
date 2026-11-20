package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils
import java.util.UUID

internal object GatemakyrShipPortals {
    private class Provider(portals: Class<*>) {
        private val mouth = Class.forName("org.valkyrienskies.mod.api.portals.ShipPortalMouth")
        private val pair = Class.forName("org.valkyrienskies.mod.api.portals.ShipPortalPair")
        private val makeMouth = mouth.getConstructor(Vec3::class.java, Vec3::class.java, Vec3::class.java)
        private val makePair = pair.getConstructor(mouth, mouth, Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
        val supported = portals.getMethod("isSupported")
        val set = portals.getMethod("set", ServerLevel::class.java, ResourceLocation::class.java, pair)
        val mounted = portals.methods.firstOrNull { it.name == "set" && it.parameterTypes.contentEquals(
            arrayOf(ServerLevel::class.java, ResourceLocation::class.java, pair, Long::class.javaObjectType, Long::class.javaObjectType)) }
        val remove = portals.getMethod("remove", ServerLevel::class.java, ResourceLocation::class.java)

        fun definition(a: GatemakyrTarget, b: GatemakyrTarget): Any {
            fun mouth(target: GatemakyrTarget): Any {
                val frame = target.localFrame()
                return makeMouth.newInstance(frame.position, frame.u, frame.v)
            }
            return makePair.newInstance(mouth(a), mouth(b), a.width / 2.0, a.height / 2.0)
        }
    }

    private val provider = try {
        Provider(Class.forName("org.valkyrienskies.mod.api.portals.ShipPortals"))
    } catch (_: ClassNotFoundException) {
        null
    }

    private val recordSupport = EntityShipCollisionUtils::class.java.methods.firstOrNull {
        it.name == "recordSupportingShip" && it.parameterTypes.contentEquals(arrayOf(Entity::class.java, Long::class.javaObjectType))
    }

    fun retainDeck(entity: Entity, shipId: Long): Boolean = recordSupport?.invoke(null, entity, shipId) != null

    private fun key(owner: UUID) = ResourceLocation("clockwork", "gatemakyr/$owner")

    fun set(level: ServerLevel, owner: UUID, a: GatemakyrTarget, b: GatemakyrTarget): Boolean {
        val api = provider ?: return false
        if (api.supported.invoke(null) != true) return false
        val carrierA = a.shipId.takeIf { it >= 0 }
        val carrierB = b.shipId.takeIf { it >= 0 }
        if (carrierA == null && carrierB == null) api.set.invoke(null, level, key(owner), api.definition(a, b))
        else (api.mounted ?: return false).invoke(null, level, key(owner), api.definition(a, b), carrierA, carrierB)
        return true
    }

    fun remove(level: ServerLevel, owner: UUID) {
        provider?.remove?.invoke(null, level, key(owner))
    }
}
