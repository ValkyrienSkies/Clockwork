package org.valkyrienskies.clockwork.util.render

import com.simibubi.create.foundation.ponder.PonderWorld
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.impl.DuctNodeInfo
import org.valkyrienskies.kelvin.impl.client.ClientKelvinInfo
import org.valkyrienskies.kelvin.util.KelvinChunkPos

class VirtualDuctNetwork(
    override var disabled: Boolean = true,
    override val edges: HashMap<Pair<DuctNodePos, DuctNodePos>, DuctEdge> = HashMap(),
    override val nodeInfo: HashMap<DuctNodePos, DuctNodeInfo> = HashMap(),
    override val nodes: HashMap<DuctNodePos, DuctNode> = HashMap(),
    override val nodesByChunk: HashMap<KelvinChunkPos, HashSet<DuctNodePos>> = HashMap(),
    override val nodesInDimension: HashMap<ResourceLocation, HashSet<DuctNodePos>> = HashMap(),
    override val unloadedNodes: HashSet<DuctNodePos> = HashSet(),
) : DuctNetwork<PonderWorld> {
    override fun addNode(pos: DuctNodePos, node: DuctNode) {
        TODO("Not yet implemented")
    }

    override fun dump() {
        disabled = true
        edges.clear()
        nodeInfo.clear()
        nodes.clear()
        nodesByChunk.clear()
        nodesInDimension.clear()
        unloadedNodes.clear()
    }

    override fun getEdgeBetween(from: DuctNodePos, to: DuctNodePos): DuctEdge? {
        TODO("Not yet implemented")
    }

    override fun getFlowBetween(from: DuctNodePos, to: DuctNodePos): Double {
        TODO("Not yet implemented")
    }

    override fun getGasMassAt(node: DuctNodePos): HashMap<GasType, Double> {
        TODO("Not yet implemented")
    }

    override fun getHeatEnergy(pos: DuctNodePos): Double {
        TODO("Not yet implemented")
    }

    override fun getNodeAt(pos: DuctNodePos): DuctNode? {
        TODO("Not yet implemented")
    }

    override fun getPressureAt(node: DuctNodePos): Double {
        TODO("Not yet implemented")
    }

    override fun getTemperatureAt(node: DuctNodePos): Double {
        TODO("Not yet implemented")
    }

    override fun markChunkLoaded(pos: KelvinChunkPos) {
        TODO("Not yet implemented")
    }

    override fun markChunkUnloaded(pos: KelvinChunkPos) {
        TODO("Not yet implemented")
    }

    override fun markLoaded(pos: DuctNodePos) {
        TODO("Not yet implemented")
    }

    override fun markUnloaded(pos: DuctNodePos) {
        TODO("Not yet implemented")
    }

    override fun removeNode(pos: DuctNodePos) {
        TODO("Not yet implemented")
    }

    override fun tick(level: PonderWorld, subSteps: Int) {
        TODO("Not yet implemented")
    }

    override fun sync(level: PonderWorld?, info: ClientKelvinInfo, chunkFlag: Boolean, player: Player?) {
    }
}