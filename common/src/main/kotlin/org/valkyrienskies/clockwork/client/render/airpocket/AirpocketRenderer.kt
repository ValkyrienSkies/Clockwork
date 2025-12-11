package org.valkyrienskies.clockwork.client.render.airpocket

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.model.BoatModel
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.BoatRenderer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.Block
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.Vector3ic
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.mixin.MixinWaterBlock
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
import org.valkyrienskies.mod.api.toMinecraft
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
    val buffer = Minecraft.getInstance().renderBuffers().bufferSource()

    inline fun AABBic.forEachBlock(fn: (Int, Int, Int) -> Unit) {
        for (x in minX() .. maxX()-1) {
            for (y in minY() .. maxY()-1) {
                for (z in minZ() .. maxZ()-1) {
                    fn(x, y, z)
                }
            }
        }
    }


    @JvmStatic
    fun getEveryNode(level: ServerLevel, ship: Ship): MutableSet<BlockPos> {
        val nodes = mutableSetOf<BlockPos>()
        ship.shipAABB!!.forEachBlock { x,y,z -> if ( level.shipObjectWorld.isIsolatedAir(x,y,z,level.dimensionId) == ConnectionStatus.UNKNOWN) nodes.add(BlockPos(x,y,z)) }
        println("\ngot nodes: $nodes")
        return nodes
    }


    fun drawQuad(buffer: VertexConsumer, matrix: Matrix4f, ship: ClientShip, a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d) {
        val sA = ship.renderTransform.shipToWorld.transformPosition(a, Vector3d()).toMinecraft().toVector3f()
        val sB = ship.renderTransform.shipToWorld.transformPosition(b, Vector3d()).toMinecraft().toVector3f()
        val sC = ship.renderTransform.shipToWorld.transformPosition(c, Vector3d()).toMinecraft().toVector3f()
        val sD = ship.renderTransform.shipToWorld.transformPosition(d, Vector3d()).toMinecraft().toVector3f()
        //println("$a $sA    $b $sB")
        //
        // val light = 0
        buffer.vertex(matrix, sA.x, sA.y, sA.z).color(0,255,0,10).endVertex()
        buffer.vertex(matrix, sB.x, sB.y, sB.z).color(0,255,0,10).endVertex()
        buffer.vertex(matrix, sC.x, sC.y, sC.z).color(0,255,0,10).endVertex()
        buffer.vertex(matrix, sD.x, sD.y, sD.z).color(0,255,0,10).endVertex()
    }

    fun renderCube(buffer: VertexConsumer, matrix: Matrix4f, ship: ClientShip, pos: BlockPos) {
        val start = pos.toJOMLD()

        drawQuad(buffer, matrix, ship, start, start.add(0.0,1.0,0.0, Vector3d()), start.add(1.0, 1.0,0.0, Vector3d()), start.add(1.0,0.0,0.0, Vector3d()))
        drawQuad(buffer, matrix, ship, start, start.add(0.0,1.0,0.0, Vector3d()), start.add(0.0, 1.0,1.0, Vector3d()), start.add(0.0,0.0,1.0, Vector3d()))
        drawQuad(buffer, matrix, ship, start, start.add(1.0,0.0,0.0, Vector3d()), start.add(1.0, 0.0,1.0, Vector3d()), start.add(0.0,0.0,1.0, Vector3d()))

        drawQuad(buffer, matrix, ship, start.add(0.0,1.0,0.0, Vector3d()), start.add(1.0,1.0,0.0, Vector3d()), start.add(1.0, 1.0,1.0, Vector3d()), start.add(0.0,1.0,1.0, Vector3d()))
        drawQuad(buffer, matrix, ship, start.add(0.0,0.0,1.0, Vector3d()), start.add(0.0,1.0,1.0, Vector3d()), start.add(1.0, 1.0,1.0, Vector3d()), start.add(1.0,0.0,1.0, Vector3d()))
        drawQuad(buffer, matrix, ship, start.add(1.0,0.0,0.0, Vector3d()), start.add(1.0,1.0,0.0, Vector3d()), start.add(1.0, 1.0,1.0, Vector3d()), start.add(1.0,0.0,1.0, Vector3d()))
    }

    @JvmStatic
    fun render(level: ClientLevel, poseStack: PoseStack, camera: Camera) {

        //if (Nodes.isNotEmpty())
        //println(Nodes)

        val cameraPos = camera.position


        poseStack.pushPose()
        poseStack.translate(-cameraPos.x,-cameraPos.y,-cameraPos.z)
        val matrix = poseStack.last().pose()


        val waterBuffer = buffer.getBuffer(RenderType.debugQuads())

        for (node in Nodes) {
            val ship = level.getShipManagingPos(node) as? ClientShip ?: continue
            renderCube(waterBuffer, matrix, ship, node)
        }

        poseStack.popPose()
    }




    fun tick(level: ServerLevel) {
        level.players().forEach { player ->
            val pos = Vector3d(player.x, player.y, player.z)
            val dragInfo = (player as IEntityDraggingInformationProvider).draggingInformation
            if (dragInfo.isEntityBeingDraggedByAShip()) {
                val shipData = level.shipWorld!!.loadedShips.getById(dragInfo.lastShipStoodOn!!)
                if (shipData != null) {
                    val nodes = getEveryNode(level, shipData)
                    println("send packet: $nodes")
                    ClockworkPackets.sendToServer(AirpocketSyncPacket(nodes))
                }
            }
        }
    }


}