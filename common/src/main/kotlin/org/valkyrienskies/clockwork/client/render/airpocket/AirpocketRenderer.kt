package org.valkyrienskies.clockwork.client.render.airpocket

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.mod.api.getShipManagingBlock
import org.valkyrienskies.mod.api.shipWorld
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider

object AirpocketRenderer {

    val Nodes = mutableSetOf<Vector3dc>()


    @JvmStatic
    fun render(level: ClientLevel, poseStack: PoseStack, camera: Camera) {
        Minecraft.getInstance().player
    }

    fun tick(level: ServerLevel) {
        level.players().forEach { player ->
            val pos = Vector3d(player.x, player.y, player.z)

            val dragInfo = (player as IEntityDraggingInformationProvider).draggingInformation
            if (dragInfo.isEntityBeingDraggedByAShip()) {
                val shipData = level.shipWorld!!.loadedShips.getById(dragInfo.lastShipStoodOn!!)
                if (shipData != null) {
                    dragInfo.
                }
            }
        }
    }

}