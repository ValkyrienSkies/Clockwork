package org.valkyrienskies.clockwork.platform

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.entry.EntityEntry
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.world.item.BlockItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandHandler
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity
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
        get() {
            throw AssertionError()
        }

    @get:ExpectPlatform
    @JvmStatic
    val ballast: BlockEntityEntry<BallastBlockEntity>
        get() {
            throw AssertionError()
        }



    @get:ExpectPlatform
    @JvmStatic
    val gravitronHandler: GravitronHandler
        get() {
            throw AssertionError()
        }

    @get:ExpectPlatform
    @JvmStatic
    val wanderwandHandler: WanderwandHandler
        get() {
            throw AssertionError()
        }

    @get:ExpectPlatform
    @JvmStatic
    val auricHandler: WanderWandClusterRenderer
        get() {
            throw AssertionError()
        }
}