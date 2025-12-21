package org.valkyrienskies.clockwork

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.curiosities.altmeter.UpdateAltMeterPacket
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserSyncPacket
import org.valkyrienskies.clockwork.content.contraptions.phys.slicker.SlickerAttachmentSyncPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronDisassemblyPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronDialPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronLeftClickPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandRenderUpdatePacket
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatDrivingPacket
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.UpdateSeatRulesPacket
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctEdgeSyncPacket
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterClosePacket
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterScreenOpenPacket
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorPacket
import org.valkyrienskies.clockwork.content.logistics.gas.redstone.RedstoneDuctScreenPacket
import org.valkyrienskies.clockwork.content.logistics.gas.smart.SmartScreenClosePacket
import org.valkyrienskies.clockwork.content.logistics.gas.smart.SmartScreenOpenPacket
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.UpdateFrequencySlotPacket
import org.valkyrienskies.clockwork.content.physicalities.wing.BlockEntityColorPacket
import org.valkyrienskies.clockwork.platform.SharedValues.packetChannel
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.CWPacket
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.util.KNodeSyncPacket
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket
import org.valkyrienskies.clockwork.util.universal_joint.UniversalJointItemPacket
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
    UPDATE_CHUTE_SLOT_PACKET(UpdateFrequencySlotPacket::class.java, ::UpdateFrequencySlotPacket),
    CREATIVE_GENERATOR_PACKET(CreativeGeneratorPacket::class.java, ::CreativeGeneratorPacket),

    GRAVITRON_GRAB_PACKET(GravitronGrabPacket::class.java, ::GravitronGrabPacket),
    GRAVITRON_LEFT_CLICK_PACKET(GravitronLeftClickPacket::class.java, ::GravitronLeftClickPacket),
    GRAVITRON_DESTROY_PACKET(GravitronDisassemblyPacket::class.java, ::GravitronDisassemblyPacket),
    WAND_SELECTION_PACKET(WandSelectionPacket::class.java, ::WandSelectionPacket),

    // Server to Client
    COLORBLOCKENTITY(BlockEntityColorPacket::class.java, ::BlockEntityColorPacket),
    SYNCABLESTORAGE(SyncableStoragePacket::class.java, ::SyncableStoragePacket),

    SLICKERATTACHMENT(SlickerAttachmentSyncPacket::class.java, ::SlickerAttachmentSyncPacket),
    GRAVITRON_DIAL_PACKET(GravitronDialPacket::class.java, ::GravitronDialPacket),

    FILTER_SCREEN_OPEN_PACKET(FilterScreenOpenPacket::class.java, ::FilterScreenOpenPacket),
    FILTER_SCREEN_CLOSE_PACKET(FilterClosePacket::class.java, ::FilterClosePacket),
    SMART_SCREEN_OPEN_PACKET(SmartScreenOpenPacket::class.java, ::SmartScreenOpenPacket),
    SMART_SCREEN_CLOSE_PACKET(SmartScreenClosePacket::class.java, ::SmartScreenClosePacket),
    REDSTONE_DUCT_SCREEN_PACKET(RedstoneDuctScreenPacket::class.java, ::RedstoneDuctScreenPacket),

    //SYNC_TEMPERATURE(TemperatureSyncPacket::class.java, ::TemperatureSyncPacket),

    PHYSICS_INFUSER(PhysicsInfuserSyncPacket::class.java, ::PhysicsInfuserSyncPacket),

    WAND_RENDER_UPDATE_PACKET(WanderwandRenderUpdatePacket::class.java, ::WanderwandRenderUpdatePacket),

    UPDATE_DUCT_EDGE(DuctEdgeSyncPacket::class.java, ::DuctEdgeSyncPacket),

    NODE_SYNC(KNodeSyncPacket::class.java, ::KNodeSyncPacket),

    UNIVERSAL_JOINT_ITEM_PACKET(UniversalJointItemPacket::class.java, ::UniversalJointItemPacket)
    ;

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
        fun sendTo(packet: S2CCWPacket?, player: ServerPlayer?) {
            packetChannel.sendTo(packet!!, player!!)
        }


        @JvmStatic
        fun init() {

        }
    }
}

