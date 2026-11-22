package org.valkyrienskies.clockwork.api.wheel

import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.core.api.bodies.shape.RoundedContactProfile
import java.util.concurrent.ConcurrentHashMap

/** Server-side profiles keyed by tire material ID, shared by all hub sizes. */
object TireContactProfiles {
    private val profiles = ConcurrentHashMap<ResourceLocation, RoundedContactProfile>()

    @JvmStatic
    fun get(tireType: ResourceLocation): RoundedContactProfile = profiles[tireType] ?: RoundedContactProfile.DEFAULT

    /** Applies on the next hub tick if the server enables rounded contact for this tire type. */
    @JvmStatic
    fun set(tireType: ResourceLocation, profile: RoundedContactProfile) {
        if (profile == RoundedContactProfile.DEFAULT) profiles.remove(tireType)
        else profiles[tireType] = profile
    }
}
