package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.CreativeGravitronItem.Companion.grabssemble
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GravitronToolBase
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML

class GravitronLeftClickPacket : C2SCWPacket {
    var clickedPos: BlockPos? = null

    constructor(buffer: FriendlyByteBuf) {
        clickedPos = buffer.readBlockPos()
    }

    constructor(clickedPos: BlockPos) {
        this.clickedPos = clickedPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val serverPlayer = context.sender
            val level = serverPlayer.level()
            if (level is ServerLevel) {
                // We don't get ship from GravitronState, because then people can't static a ship thats not being grabbed
                val ship: LoadedServerShip? = level.getLoadedShipManagingPos(clickedPos!!)
                if (ship != null) {
                    val state = GravitronState.getState(serverPlayer)

                    if (SharedValues.gravitronHandler.isRegular) {

                        // Only do cooldown for survival gravitron
                        val stack = serverPlayer.mainHandItem
                        serverPlayer.cooldowns.addCooldown(stack.item, 20)

                        val lookDir = serverPlayer.lookAngle.normalize().toJOML()
                        val magnitude = ClockworkConfig.SERVER.survivalGravitronYeetForce * ship.inertiaData.mass
                        val launchVec = lookDir.mul(magnitude)
                        ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId).applyWorldForceToBodyPos(ship.id, launchVec, state.shipGrabbedPos!!)
                        GrabTool.dropShip(serverPlayer)
                        level.playSound(
                            serverPlayer,
                            serverPlayer.blockPosition(),
                            ClockworkSounds.GRAVITRON_LAUNCH.mainEvent!!,
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                    } else {
                        // To make sure when un-static-ing, it doesn't go back to actively grabbing
                        if (state.shipID != null) {
                            GrabTool.dropShip(serverPlayer)
                        }

                        ship.isStatic = !ship.isStatic
                        level.playSound(
                            serverPlayer,
                            serverPlayer.blockPosition(),
                            ClockworkSounds.GRAVITRON_FREEZE.mainEvent!!,
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                    }
                }

            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(clickedPos!!)
    }
}
