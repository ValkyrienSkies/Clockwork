package org.valkyrienskies.clockwork.client.render.airpocket

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.model.BoatModel
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.BoatRenderer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.Block
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.world.connectivity.ConnectionStatus
import org.valkyrienskies.core.util.x
import org.valkyrienskies.core.util.y
import org.valkyrienskies.core.util.z
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3i
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.shipWorld
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.vsCore
import org.w3c.dom.Node
import kotlin.collections.mutableSetOf


object AirpocketRenderer {

    var Nodes = mutableSetOf<BlockPos>()

    fun iterateShipBlocks(ship: Ship): Iterable<BlockPos> {
        val aabb = ship.shipAABB!!
        return BlockPos.betweenClosed(
            aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ()
        )
    }


    @JvmStatic
    fun getEveryNode(level: ServerLevel, ship: Ship): MutableSet<BlockPos> {
        val nodes = mutableSetOf<BlockPos>()
        iterateShipBlocks(ship).forEach {
            println(it)
            if (level.getBlockState(it).block is AirBlock && level.shipObjectWorld.isIsolatedAir(it.x, it.y, it.z, level.dimensionId) == ConnectionStatus.DISCONNECTED)
                nodes.add(it)
        }
        return nodes
    }


    fun drawQuad(buffer: BufferBuilder, ship: ClientShip, a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d) {
        val sA = ship.renderTransform.shipToWorld.transformPosition(a)
        val sB = ship.renderTransform.shipToWorld.transformPosition(a)
        val sC = ship.renderTransform.shipToWorld.transformPosition(a)
        val sD = ship.renderTransform.shipToWorld.transformPosition(a)

        buffer.vertex(sA.x, sA.y, sA.z)
        buffer.vertex(sB.x, sB.y, sB.z)
        buffer.vertex(sC.x, sC.y, sC.z)
        buffer.vertex(sD.x, sD.y, sD.z)
    }

    fun renderCube(buffer: BufferBuilder, ship: ClientShip, pos: BlockPos) {
        val start = pos.toJOMLD()

        drawQuad(buffer, ship, start, start.add(0.0,1.0,0.0), start.add(1.0, 1.0,0.0), start.add(1.0,0.0,0.0))
        drawQuad(buffer, ship, start, start.add(0.0,1.0,0.0), start.add(0.0, 1.0,1.0), start.add(0.0,0.0,1.0))
        drawQuad(buffer, ship, start, start.add(1.0,0.0,0.0), start.add(1.0, 0.0,1.0), start.add(0.0,0.0,1.0))

        drawQuad(buffer, ship, start.add(0.0,1.0,0.0), start.add(1.0,1.0,0.0), start.add(1.0, 1.0,1.0), start.add(0.0,1.0,1.0))
        drawQuad(buffer, ship, start.add(0.0,0.0,1.0), start.add(0.0,1.0,1.0), start.add(1.0, 1.0,1.0), start.add(1.0,0.0,1.0))
        drawQuad(buffer, ship, start.add(1.0,0.0,0.0), start.add(1.0,1.0,0.0), start.add(1.0, 1.0,1.0), start.add(1.0,0.0,1.0))
    }

    @JvmStatic
    fun render(level: ClientLevel, poseStack: PoseStack, camera: Camera) {

        //if (Nodes.isNotEmpty())
        //println(Nodes)

        for (node in Nodes) {
            val ship = level.getShipManagingPos(node) as? ClientShip ?: continue


            val tesselator = Tesselator.getInstance()
            val vBuffer = tesselator.builder

            val cameraPos = camera.position

            poseStack.pushPose()
            poseStack.translate(-cameraPos.x,-cameraPos.y,-cameraPos.z)

            RenderSystem.setShader(GameRenderer::getRendertypeWaterMaskShader)
            vBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)

            renderCube(vBuffer, ship, node)

            tesselator.end()
            poseStack.popPose()

        }
    }




    fun tick(level: ServerLevel) {
        level.players().forEach { player ->
            val pos = Vector3d(player.x, player.y, player.z)

            val dragInfo = (player as IEntityDraggingInformationProvider).draggingInformation
            if (dragInfo.isEntityBeingDraggedByAShip()) {
                val shipData = level.shipWorld!!.loadedShips.getById(dragInfo.lastShipStoodOn!!)
                if (shipData != null) {
                    val nodes = getEveryNode(level, shipData)
                    //println("send packet: $nodes")
                    ClockworkPackets.sendToServer(AirpocketSyncPacket(nodes))
                }
            }
        }
    }


}