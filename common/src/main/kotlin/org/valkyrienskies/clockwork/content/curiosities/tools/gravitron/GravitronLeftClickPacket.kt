package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD

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
            val stack = serverPlayer.mainHandItem
            val isRegular = stack.`is`(ClockworkItems.GRAVITRON.get())
            val isCreative = stack.`is`(ClockworkItems.CREATIVE_GRAVITRON.get())
            if ((!isRegular && !isCreative) || !serverPlayer.isAlive || serverPlayer.isSpectator) return@enqueueWork
            if (serverPlayer.cooldowns.isOnCooldown(stack.item)) return@enqueueWork
            if (!GrabTool.updateEquipment(serverPlayer)) return@enqueueWork
            if (level is ServerLevel) {
                val state = GravitronState.getState(serverPlayer)
                val heldShip = state.shipID?.let { level.shipObjectWorld.loadedShips.getById(it) }
                val ship = if (isRegular) heldShip ?: level.getLoadedShipManagingPos(clickedPos!!)
                    else level.getLoadedShipManagingPos(clickedPos!!) ?: heldShip
                if (ship != null) {
                    val anchor = if (state.shipID == ship.id) state.shipGrabbedPos ?: clickedPos!!.toJOMLD()
                        else clickedPos!!.toJOMLD().add(0.5, 0.5, 0.5)
                    val worldAnchor = ship.shipToWorld.transformPosition(anchor, Vector3d())
                    val range = if (isRegular) ClockworkConfig.SERVER.survivalGravitronMaxRange else 1000.0
                    if (worldAnchor.distanceSquared(serverPlayer.eyePosition.toJOML()) > range * range) return@enqueueWork
                    if (isRegular) {
                        if (ship.inertiaData.mass > ClockworkConfig.SERVER.maxGravitronMass * 1000.0) {
                            GrabTool.dropShip(serverPlayer, false)
                            GrabTool.overload(serverPlayer, ship, anchor)
                            return@enqueueWork
                        }
                        serverPlayer.cooldowns.addCooldown(stack.item, 20)

                        val lookDir = serverPlayer.lookAngle.normalize().toJOML()
                        val magnitude = ClockworkConfig.SERVER.survivalGravitronYeetForce * ship.inertiaData.mass
                        val launchVec = lookDir.mul(magnitude)
                        ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId).applyWorldForceToModelPos(ship.id, launchVec, anchor)
                        GrabTool.dropShip(serverPlayer, false)
                        GravitronAnimationPacket.send(serverPlayer, GravitronAction.LAUNCH, ship.id, anchor)
                        level.playSound(
                            null,
                            serverPlayer.blockPosition(),
                            ClockworkSounds.GRAVITRON_LAUNCH.mainEvent!!,
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                    } else {
                        // To make sure when un-static-ing, it doesn't go back to actively grabbing
                        if (state.shipID != null) {
                            GrabTool.dropShip(serverPlayer, false)
                        }

                        ship.isStatic = !ship.isStatic
                        if (ship.isStatic) GravitronFrozenShip.freeze(level, ship, anchor)
                        else GravitronFrozenShip.thaw(level, ship)
                        GravitronAnimationPacket.send(serverPlayer,
                            if (ship.isStatic) GravitronAction.FREEZE else GravitronAction.UNFREEZE, ship.id, anchor)
                        level.playSound(
                            null,
                            serverPlayer.blockPosition(),
                            ClockworkSounds.GRAVITRON_FREEZE.mainEvent!!,
                            SoundSource.PLAYERS,
                            1f,
                            if (ship.isStatic) 1f else 0.75f
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
