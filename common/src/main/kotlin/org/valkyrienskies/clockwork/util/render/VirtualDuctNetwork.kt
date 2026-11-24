package org.valkyrienskies.clockwork.util.render

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import net.createmod.ponder.api.level.PonderLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.IClockworkNodeBE
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlock
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlockEntity
import org.valkyrienskies.kelvin.KelvinMod.KELVINLOGGER
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.api.nodes.ILeakNode
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import org.valkyrienskies.kelvin.impl.DuctNodeInfo
import org.valkyrienskies.kelvin.impl.client.ClientKelvinInfo
import org.valkyrienskies.kelvin.impl.recipe.KelvinReactionDataLoader
import org.valkyrienskies.kelvin.impl.registry.GasParticlePickerRegistry
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.serialization.NodeNBTUtil
import org.valkyrienskies.kelvin.util.GasExplosionDamageCalculator
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.KelvinChunkPos
import org.valkyrienskies.kelvin.util.KelvinDamageSources
import org.valkyrienskies.kelvin.util.KelvinExtensions.toChunkPos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VirtualDuctNetwork(
    override var disabled: Boolean = false,
    override val edges: HashMap<Pair<DuctNodePos, DuctNodePos>, DuctEdge> = HashMap(),
    override val nodeInfo: HashMap<DuctNodePos, DuctNodeInfo> = HashMap(),
    override val nodes: HashMap<DuctNodePos, DuctNode> = HashMap(),
    override val nodesByChunk: HashMap<KelvinChunkPos, HashSet<DuctNodePos>> = HashMap(),
    override val nodesInDimension: HashMap<ResourceLocation, HashSet<DuctNodePos>> = HashMap(),
    override val unloadedNodes: HashSet<DuctNodePos> = HashSet(),
) : DuctNetwork<PonderLevel> {
    var solver: KelvinSolver = ClockworkConfig.KELVIN.kelvinSolver.getSolver()
    internal var sceneReactions: Collection<GasBaseRecipe>? = null
    private val pendingBlocks = linkedSetOf<BlockPos>()

    fun queueUpdate(pos: BlockPos) {
        pendingBlocks.add(pos.immutable())
    }

    internal fun updateBlocks(level: PonderLevel) {
        if (pendingBlocks.isEmpty()) return
        val changed = pendingBlocks.toList()
        pendingBlocks.clear()

        val reconnect = linkedSetOf<BlockPos>()
        for (pos in changed) {
            reconnect.add(pos)
            Direction.entries.forEach { reconnect.add(pos.relative(it)) }
            val block = level.getBlockState(pos).block as? INodeBlock ?: continue
            val be = level.getBlockEntity(pos)
            val nodePos = pos.toDuctNodePos(level.dimension().location())
            if (be is DuctTankBlockEntity && !be.isController) continue
            if (nodePos !in nodes) {
                val node = if (block is DuctTankBlock && be is DuctTankBlockEntity) {
                    block.createTankNode(nodePos, (be.width * be.width * be.height).toDouble())
                } else block.createNode(nodePos)
                addNode(nodePos, node)
            }
            if (be is IClockworkNodeBE) {
                be.dataToLoad?.let { loadNodeData(nodePos, it) }
                be.dataToLoad = null
            }
            nodes[nodePos]?.nodeEdges?.toList()?.forEach { removeEdge(it.nodeA, it.nodeB) }
        }

        for (pos in reconnect) {
            val state = level.getBlockState(pos)
            val block = state.block
            if (block is DuctBlock) {
                val connected = block.getConnectedState(level, state, pos)
                if (connected != null && connected != state) level.setBlockAndUpdate(pos, connected)
            } else if (block is INodeBlock) {
                (level.getBlockEntity(pos) as? IConnectable)?.refreshConnections(level, pos)
            }
        }
    }

    fun saveNodeData(pos: DuctNodePos, tag: CompoundTag) {
        val info = nodeInfo[pos] ?: return
        val data = CompoundTag()
        NodeNBTUtil.serializeNode(pos, this, data)
        data.putDouble("KelvinVolume", info.totalVolume)
        data.putDouble("KelvinPressure", info.currentPressure)
        data.putDouble("KelvinEnergy", info.currentEnergy)
        tag.put("kelvin_node_data", data)
    }

    private fun loadNodeData(pos: DuctNodePos, tag: CompoundTag) {
        val data = tag.getCompound("kelvin_node_data")
        if (data.isEmpty) return
        val info = nodeInfo.getValue(pos)
        info.currentGasMasses.clear()
        for ((id, gas) in GasTypeRegistry.GAS_TYPES) {
            if (data.contains(id.toString())) info.currentGasMasses[gas] = data.getDouble(id.toString())
        }
        if (data.contains("KelvinTemperature")) info.currentTemperature = data.getDouble("KelvinTemperature")
        info.currentPressure = data.getDouble("KelvinPressure")
        info.previousPressure = info.currentPressure
        val volume = data.getDouble("KelvinVolume")
        if (volume > 0.0) {
            info.totalVolume = volume
            info.volumeChange = volume - nodes.getValue(pos).volume
            info.previousVolumeChange = info.volumeChange
        }
        info.currentEnergy = if (data.contains("KelvinEnergy") && !data.contains("KelvinWallTemperature")) {
            data.getDouble("KelvinEnergy")
        } else {
            getNodeHeatCapacity(pos) * info.currentTemperature
        }
    }

    override fun createGasParticle(
        level: PonderLevel, gasType: GasType, pos: DuctNodePos,
        x: Double, y: Double, z: Double,
        xSpeed: Double, ySpeed: Double, zSpeed: Double
    ) {
        val particleTypePicker = GasParticlePickerRegistry.getParticlePicker(gasType) ?: return KELVINLOGGER.error("${gasType.resourceLocation} lacks a ParticlePicker")
        val particleOptions = particleTypePicker.chooseParticleOptions(level, pos)
        level.addParticle(particleOptions, x, y, z, xSpeed, ySpeed, zSpeed)
    }

    override fun markLoaded(pos: DuctNodePos) {
        if (!unloadedNodes.remove(pos)) return
        nodes[pos]?.nodeEdges?.forEach { edge ->
            edge.unloaded = edge.nodeA in unloadedNodes || edge.nodeB in unloadedNodes
        }
    }

    override fun markUnloaded(pos: DuctNodePos) {
        val node = nodes[pos] ?: return
        if (!unloadedNodes.add(pos)) return
        for (edge in node.nodeEdges.toList()) {
            val other = if (edge.nodeA == pos) edge.nodeB else edge.nodeA
            if (other in unloadedNodes) {
                removeEdge(pos, other)
                if (nodes[other]?.nodeEdges?.isEmpty() == true) removeNode(other)
            } else {
                edge.unloaded = true
            }
        }
        if (node.nodeEdges.isEmpty()) removeNode(pos)
    }

    override fun markChunkLoaded(pos: KelvinChunkPos) {
        if (nodesByChunk.contains(pos)) {
            return
        }
        nodesByChunk[pos] = hashSetOf()
    }

    override fun markChunkUnloaded(pos: KelvinChunkPos) {
        if (!nodesByChunk.contains(pos)) {
            return
        }
        nodesByChunk.remove(pos)
    }

    override fun getFlowBetween(from: DuctNodePos, to: DuctNodePos): Double {
        val edge = getEdgeBetween(from, to) ?: return 0.0
        return edge.currentFlowRate
    }

    override fun getPressureAt(node: DuctNodePos): Double {
        if (nodeInfo[node]?.currentPressure?.isNaN() == true) return 0.0
        return nodeInfo[node]?.currentPressure ?: 0.0
    }

    override fun getTemperatureAt(node: DuctNodePos): Double {
        return nodeInfo[node]?.currentTemperature ?: 0.0001
    }

    override fun getGasMassAt(node: DuctNodePos): Map<GasType, Double> {
        return nodeInfo[node]?.currentGasMasses ?: emptyMap()
    }

    override fun getEdgeBetween(from: DuctNodePos, to: DuctNodePos): DuctEdge? {
        return edges[Pair(from, to)] ?: edges[Pair(to, from)]
    }

    override fun getNodeAt(pos: DuctNodePos): DuctNode? {
        return nodes[pos]
    }

    override fun addNode(pos: DuctNodePos, node: DuctNode) {
        if (nodes[pos]?.javaClass == node.javaClass) {
            markLoaded(pos)
            return
        }
        if (pos in nodes) removeNode(pos)
        nodes[pos] = node
        // Seed wall thermal energy at ambient (273.15K) so a fresh node doesn't act as a
        // 0K cold sink for the first gas to enter. Combined energy = wallCap * T_ambient.
        nodeInfo[pos] = DuctNodeInfo(273.15, 0.0, Object2DoubleOpenHashMap(), node.volume, currentEnergy = node.heatCapacity * 273.15)
        nodesInDimension.getOrPut(pos.dimensionId) { hashSetOf() }.add(pos)
        nodesByChunk.getOrPut(pos.toChunkPos()) { hashSetOf() }.add(pos)
        KELVINLOGGER.debug("Added node at {}", pos)
    }

    override fun removeNode(pos: DuctNodePos) {
        nodes[pos]?.nodeEdges?.toList()?.forEach { removeEdge(it.nodeA, it.nodeB) }
        nodes.remove(pos)
        nodeInfo.remove(pos)
        unloadedNodes.remove(pos)
        nodesInDimension[pos.dimensionId]?.remove(pos)
        nodesByChunk[pos.toChunkPos()]?.remove(pos)
    }

    override fun addEdge(posA: DuctNodePos, posB: DuctNodePos, edge: DuctEdge) {
        if (posA == posB || posA !in nodes || posB !in nodes) return
        removeEdge(posA, posB)
        edge.unloaded = posA in unloadedNodes || posB in unloadedNodes
        edges[posA to posB] = edge
        nodes.getValue(posA).nodeEdges.add(edge)
        nodes.getValue(posB).nodeEdges.add(edge)
    }

    override fun removeEdge(posA: DuctNodePos, posB: DuctNodePos) {
        val edge = edges.remove(Pair(posA, posB)) ?: edges.remove(Pair(posB, posA))
        if (edge != null) {
            nodes[posA]?.nodeEdges?.remove(edge)
            nodes[posB]?.nodeEdges?.remove(edge)
            KELVINLOGGER.debug("Removed edge between {} and {}", posA, posB)
        }
    }

    override fun modTemperature(pos: DuctNodePos, deltaTemperature: Double) {
        if (deltaTemperature.isNaN() || deltaTemperature.isInfinite()) {
            nodeInfo[pos]?.currentTemperature = 0.0001
            return
        }
        nodeInfo[pos]?.currentTemperature = max(nodeInfo[pos]?.currentTemperature?.plus(deltaTemperature) ?: 0.0001, 0.0001)
        // update thermal energy using combined gas+wall capacity
        val capacity = getNodeHeatCapacity(pos)
        nodeInfo[pos]?.currentEnergy = nodeInfo[pos]?.currentTemperature?.times(capacity) ?: 0.0001
    }

    override fun modPressure(pos: DuctNodePos, deltaPressure: Double) {
        nodeInfo[pos]?.currentPressure = nodeInfo[pos]?.currentPressure?.plus(deltaPressure) ?: 0.0
    }

    override fun modGasMass(pos: DuctNodePos, gasType: GasType, deltaMass: Double) {
        nodeInfo[pos]?.currentGasMasses?.put(gasType, nodeInfo[pos]?.currentGasMasses?.get(gasType)?.plus(deltaMass) ?: deltaMass)
    }

    override fun modGasMassOfTemperature(pos: DuctNodePos, gasType: GasType, deltaMass: Double, gasTemperature: Double ) {
        val info = nodeInfo[pos] ?: return
        val nodeCapacity = getNodeHeatCapacity(pos) // gas + wall, J/K
        val gasCv = (gasType.specificHeatCapacity / gasType.adiabaticIndex) * 1000.0 // J/(kg·K)
        val addedGasCapacity = deltaMass * gasCv

        val newCapacity = nodeCapacity + addedGasCapacity
        val newTemperature = if (newCapacity > 1e-12) {
            (nodeCapacity * info.currentTemperature + addedGasCapacity * gasTemperature) / newCapacity
        } else {
            info.currentTemperature
        }

        info.currentTemperature = max(newTemperature, 0.0001)
        modGasMass(pos, gasType, deltaMass)
        info.currentEnergy = info.currentTemperature * getNodeHeatCapacity(pos)
    }

    override fun getHeatEnergy(pos: DuctNodePos): Double {
        return nodeInfo[pos]?.currentEnergy ?: 0.0
    }

    override fun modHeatEnergy(pos: DuctNodePos, deltaEnergy: Double) {
        if (deltaEnergy.isNaN() || deltaEnergy.isInfinite()) {
            return
        }
        val energy = getHeatEnergy(pos)
        val result = (energy+deltaEnergy).coerceAtLeast(0.001)

        nodeInfo[pos]?.currentEnergy = result
    }

    override fun modVolume(pos: DuctNodePos, deltaVolume: Double) {
        nodeInfo[pos]?.volumeChange = nodeInfo[pos]?.volumeChange?.plus(deltaVolume) ?: 0.0
    }

    override fun addGas(pos: DuctNodePos, gasType: GasType, amount: Double, energyDelta: Double): Boolean {
        val node = nodes[pos] ?: return false
        nodeInfo[pos]?.currentGasMasses?.put(gasType, nodeInfo[pos]?.currentGasMasses?.get(gasType)?.plus(amount) ?: amount)
        modHeatEnergy(pos, energyDelta)
        return true
    }

    override fun addGasAtTemperature(pos: DuctNodePos, gasType: GasType, amount: Double, temperature: Double): Boolean {
        val node = nodes[pos] ?: return false
        val specificHeat = (gasType.specificHeatCapacity * 1000.0) / gasType.adiabaticIndex
        val energyToAdd = amount * specificHeat * temperature
        nodeInfo[pos]?.currentGasMasses?.put(gasType, nodeInfo[pos]?.currentGasMasses?.get(gasType)?.plus(amount) ?: amount)
        modHeatEnergy(pos, energyToAdd)
        return true
    }

    override fun removeGas(pos: DuctNodePos, gasType: GasType, amount: Double): Boolean {
        val node = nodes[pos] ?: return false
        var amountToRemove = amount
        val currentAmount = nodeInfo[pos]?.currentGasMasses?.get(gasType) ?: 0.0
        if (currentAmount < amount) {
            amountToRemove = currentAmount
        }
        val sourceTemp = getTemperatureAt(pos)
        // now, let's make sure we remove the appropriate amount of thermal energy from the system
        // (bare-gas cv: the wall stays in place when gas is removed)
        val cv = (gasType.specificHeatCapacity * 1000.0) / gasType.adiabaticIndex
        var energyToRemove = amountToRemove * cv * sourceTemp
        if (energyToRemove.isNaN() || energyToRemove.isInfinite()) {
            energyToRemove = 0.0
        }
        if (energyToRemove > getHeatEnergy(pos)) {
            energyToRemove = getHeatEnergy(pos)
        }
        modGasMass(pos, gasType, -amountToRemove)
        modHeatEnergy(pos, -energyToRemove)
        return true
    }


    override fun tick(level: PonderLevel, subSteps: Int) {
        if (disabled) return

        updateBlocks(level)
        val dimensionNodes = nodesInDimension[level.dimension().location()] ?: return
        if (dimensionNodes.isEmpty()) return
        solver.step(this, subSteps)

        val explnodes = HashMap<DuctNodePos, Double>()
        val melted = HashSet<DuctNodePos>()

        val nodeInfoToProcess = HashMap(nodeInfo)
        for (nodePos in nodeInfoToProcess.keys) {
            if (nodeInfo[nodePos] == null || nodes[nodePos] == null) continue


            val node = nodes[nodePos]!!
            val info = nodeInfo[nodePos]!!

            if (info.currentPressure > node.maxPressure) {
                explnodes[nodePos] = abs(info.currentPressure - node.maxPressure)
                KELVINLOGGER.info("Node at $nodePos exploded due to Overpressure. Pressure at time of failure: ${info.currentPressure}")
            }

            if (info.currentTemperature > node.maxTemperature) {
                melted.add(nodePos)
                KELVINLOGGER.info("Node at $nodePos reached its Melting Point. Temperature at time of failure: ${info.currentTemperature}")
            }

            if (node is ILeakNode) {
                val ratio = (node as ILeakNode).getLeakRatio(level)
                for ((gas, value) in getGasMassAt(nodePos)) removeGas(nodePos, gas, value*ratio)

            }
        }
//            if (info.currentPressure < node.minPressure) {
//                // todo wuh oh spaghettio prepare to implodeio
//            }
        //copilot wrote this so im immortalizing it

        explnodes.forEach { pos, pressureExcess ->
            level.explode(null, KelvinDamageSources.gasExplosion(level.registryAccess(), null), GasExplosionDamageCalculator(pressureExcess),pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, min(max(1.0, pressureExcess / 10000.0), 10.0).toFloat(), true, Level.ExplosionInteraction.TNT)
        }

        melted.forEach {
            level.destroyBlock(it.toMinecraft(), true)
        }


        val reactions = sceneReactions ?: KelvinReactionDataLoader.gas_reactions.values
        // Process recipes
        for (node in dimensionNodes) {
            val gasMasses = getGasMassAt(node)
            if (gasMasses.isEmpty()) continue

            val state = getNodeState(node) ?: continue

            for (reaction in reactions) {
                var con = false
                reaction.requirements.forEach {if (!it.key.apply_requirement(level, state, it.value)) { con = true; return@forEach }}
                if (con) continue

                calcReaction(node, gasMasses, reaction.gasses, reaction.result, reaction.energy)
            }
        }
    }

    private fun calcReaction(ductNodePos: DuctNodePos, gasMasses: Map<GasType, Double>, inputGasses: Map<GasType, Double>, outputGasses: Map<GasType, Double>, deltaEnergy: Double) {

        var reactionAmount = Double.MAX_VALUE
        for (gas in inputGasses) {
            if (gas.key !in gasMasses || gasMasses[gas.key]!! < 0.0001) return

            val thisOutput =  gasMasses[gas.key]!! / gas.value
            if (thisOutput < reactionAmount) reactionAmount = thisOutput
        }

        for (gas in inputGasses) modGasMass(ductNodePos,gas.key,-reactionAmount * gas.value)

        for (gas in outputGasses) modGasMass(ductNodePos,gas.key,reactionAmount * gas.value)

        modHeatEnergy(ductNodePos, deltaEnergy * reactionAmount)

    }

    override fun dump() {
        KELVINLOGGER.info("Disabling PonderKelvin...")

        disabled = true

        KELVINLOGGER.info("Dumping PonderKelvin information...")

        edges.clear()
        nodes.clear()
        nodeInfo.clear()

        unloadedNodes.clear()
        nodesByChunk.clear()
        nodesInDimension.clear()
        pendingBlocks.clear()

        KELVINLOGGER.info("Dumped PonderKelvin information. Now get out!")
    }

    override fun sync(
        level: PonderLevel?,
        info: ClientKelvinInfo,
        chunkFlag: Boolean,
        player: Player?
    ) {}
}
