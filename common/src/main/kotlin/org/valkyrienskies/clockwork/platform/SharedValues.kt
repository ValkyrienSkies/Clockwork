package org.valkyrienskies.clockwork.platform

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.tterrag.registrate.util.entry.EntityEntry
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel
import java.util.function.BiConsumer

object SharedValues {
    @get:ExpectPlatform
    @JvmStatic
    val packetChannel: PacketChannel
        get() {
            throw AssertionError()
        }

    @ExpectPlatform
    @JvmStatic
    fun customRenderedRegisterer(): BiConsumer<CWItem, CustomRenderedItemModelRenderer> {
        throw AssertionError()
    }

    @ExpectPlatform
    @JvmStatic
    fun customBlockItemRenderedRegisterer(): BiConsumer<BlockItem, CustomRenderedItemModelRenderer> {
        throw AssertionError()
    }

    @get:ExpectPlatform
    @JvmStatic
    val sequencedSeat: EntityEntry<SequencedSeatEntity>
        //region Entities
        get() {
            throw AssertionError()
        }

    @get:ExpectPlatform
    @JvmStatic
    val frostingBuckets: ArrayList<Item>
        //endregion
        get() {
            throw AssertionError()
        }
}