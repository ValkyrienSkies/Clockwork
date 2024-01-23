package org.valkyrienskies.clockwork

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.UpdateAltMeterPacket
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.UpdateGyroPacket
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserSyncPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorSelectionPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronDestroyPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatDrivingPacket
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.UpdateSeatRulesPacket
import org.valkyrienskies.clockwork.content.logistics.heat.usage.gas_nozzle.TempGasNozzleSyncPacket
import org.valkyrienskies.clockwork.content.physicalities.wing.BlockEntityColorPacket
import org.valkyrienskies.clockwork.platform.SharedValues.packetChannel
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.CWPacket
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket
import java.util.function.Function

@Suppress("UNCHECKED_CAST")
enum class ClockworkPackets(
    private val type: Class<out CWPacket>,
    private val factory: Function<FriendlyByteBuf, out CWPacket>
) {
    // Client to Server
    UPDATE_SEAT_RULES(UpdateSeatRulesPacket::class.java, ::UpdateSeatRulesPacket),
    SEQUENCER_SEAT_DRIVING(SequencedSeatDrivingPacket::class.java, ::SequencedSeatDrivingPacket),
    UPDATE_ALT_METER(UpdateAltMeterPacket::class.java, ::UpdateAltMeterPacket),
    UPDATE_GYRO(UpdateGyroPacket::class.java, ::UpdateGyroPacket),

    GRAVITRON_GRAB_PACKET(GravitronGrabPacket::class.java, ::GravitronGrabPacket),
    GRAVITRON_DESTROY_PACKET(GravitronDestroyPacket::class.java, ::GravitronDestroyPacket),

    // Server to Client
    COLORBLOCKENTITY(BlockEntityColorPacket::class.java, ::BlockEntityColorPacket),
    SYNCABLESTORAGE(SyncableStoragePacket::class.java, ::SyncableStoragePacket),
    SYNC_GAS_NOZZLE(TempGasNozzleSyncPacket::class.java, ::TempGasNozzleSyncPacket),


    PHYSICS_INFUSER(PhysicsInfuserSyncPacket::class.java, ::PhysicsInfuserSyncPacket),
    AURIC_DESIGNATOR(AuricDesignatorSelectionPacket::class.java, ::AuricDesignatorSelectionPacket);

    init {
        packetChannel.registerPacket(type as Class<CWPacket>, factory as Function<FriendlyByteBuf, CWPacket>)
    }


    companion object {

        @JvmStatic
        fun sendToNear(world: Level?, pos: BlockPos?, range: Int, message: S2CCWPacket?) {
            packetChannel.sendToNear(world!!, pos!!, range, message!!)
        }

        @JvmStatic
        fun sendToServer(packet: C2SCWPacket?) {
            packetChannel.sendToServer(packet!!)
        }

        @JvmStatic
        fun sendToClientsTracking(packet: S2CCWPacket?, entity: Entity?) {
            packetChannel.sendToClientsTracking(packet!!, entity!!)
        }

        @JvmStatic
        fun sendToClientsTrackingAndSelf(packet: S2CCWPacket?, player: ServerPlayer?) {
            packetChannel.sendToClientsTrackingAndSelf(packet!!, player!!)
        }

        @JvmStatic
        fun init() {

        }
    }
}

