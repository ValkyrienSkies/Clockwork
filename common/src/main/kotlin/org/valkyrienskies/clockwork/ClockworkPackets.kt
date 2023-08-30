package org.valkyrienskies.clockwork

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserSyncPacket
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.AreaDesignatorSelectionPacket
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.CWPacket
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket


enum class ClockworkPackets(type: Class<T>, factory: java.util.function.Function<FriendlyByteBuf, CWPacket>) {
    // Client to Server
    UPDATE_SEAT_RULES(UpdateSeatRulesPacket::class.java, ::UpdateSeatRulesPacket),
    SEQUENCER_SEAT_DRIVING(SequencedSeatDrivingPacket::class.java, ::SequencedSeatDrivingPacket),

    // Server to Client
    COLORBLOCKENTITY(BlockEntityColorPacket::class.java, ::BlockEntityColorPacket),
    //AFTERBLAZERSTATUS(AfterblazerStatusPacket::class.java, ::AfterblazerStatusPacket),

    // auric designator
    DESIGNATORSELECTION(AreaDesignatorSelectionPacket::class.java, ::AreaDesignatorSelectionPacket),

    // INSERT CLUSTER HANDLING PACKET STUFF HERE
    PHYSICSINFUSERUPDATE(
        PhysicsInfuserSyncPacket::class.java,
        ::PhysicsInfuserSyncPacket);

    init {
        SharedValues.packetChannel.registerPacket(type, factory)
    }

    companion object {
        // Force the class to load
        fun init() {}
        fun sendToNear(world: Level, pos: BlockPos, range: Int, message: S2CCWPacket) {
            SharedValues.packetChannel.sendToNear(world, pos, range, message)
        }

        fun sendToServer(packet: C2SCWPacket) {
            SharedValues.packetChannel.sendToServer(packet)
        }

        fun sendToClientsTracking(packet: S2CCWPacket, entity: Entity) {
            SharedValues.packetChannel.sendToClientsTracking(packet, entity)
        }

        fun sendToClientsTrackingAndSelf(packet: S2CCWPacket, player: ServerPlayer) {
            SharedValues.packetChannel.sendToClientsTrackingAndSelf(packet, player)
        }
    }
}