package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.EntityEntry
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.CopterContraptionEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity
import org.valkyrienskies.clockwork.platform.SharedValues
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import org.valkyrienskies.clockwork.platform.api.DeferredRegister
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity

object ClockworkEntities {
    val SEQUENCED_SEAT: EntityEntry<SequencedSeatEntity> = SharedValues.sequencedSeat
    val COPTER_CONTRAPTION: EntityEntry<CopterContraptionEntity> = SharedValues.copterContraption
    lateinit var ROPE_KNOT: EntityType<RopeKnotEntity>
        private set

    @JvmStatic
    fun register() {
        val entities = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ClockworkMod.MOD_ID)
        entities.register("rope_knot") {
            EntityType.Builder.of(::RopeKnotEntity, MobCategory.MISC)
                .sized(0.45f, 0.45f).noSave().noSummon().clientTrackingRange(12).updateInterval(1)
                .build("vs_clockwork:rope_knot").also { ROPE_KNOT = it }
        }
        entities.registerAll()
    }
}
