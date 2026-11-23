package org.valkyrienskies.clockwork.content.curiosities.harpoon

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkDamageTypes
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkRecipes.ClockworkRecipeTypes.SIPHONING
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.logistics.gas.IClockworkNodeBE
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.configureMetalDuctEdge
import org.valkyrienskies.clockwork.util.kelvin.KNodeSyncPacket
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.PumpDuctEdge
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.toWorldCoordinates
import java.util.UUID
import kotlin.math.abs

class SiphonHarpoonBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    HarpoonBlockEntity(type, pos, state), IClockworkNodeBE, IConnectable {
    override var dataToLoad: CompoundTag? = null
    internal override val usesHose = true
    private val recipes = RecipeManager.createCheck<SiphoningRecipe.Input, SiphoningRecipe>(SIPHONING.getType())
    private var pump: PumpDuctEdge? = null
    private var rearNode: DuctNodePos? = null
    private var harvestedGas = ""
    private var kgPerTick = 0.0
    private var pumping = false
    private var damageRopeId: UUID? = null
    private var damageTicks = 0
    private var pendingDamage = 0.0

    override fun getDuctNodePosition() = level?.let { blockPos.toDuctNodePos(it.dimension().location()) } ?: blockPos.toDuctNodePos()

    override fun setLazyTickRate(slowTickRate: Int) = super.setLazyTickRate(ClockworkConfig.KELVIN.kelvinNodeBlockEntityLazyTickRate)

    override fun lazyTick() {
        super.lazyTick()
        val world = level as? ServerLevel ?: return
        val network = ClockworkMod.getKelvin(world)
        val pos = getDuctNodePosition()
        if (network.getNodeAt(pos) == null) network.addNode(pos, (blockState.block as SiphonHarpoonBlock).createNode(pos))
        if (pos in network.unloadedNodes) network.markLoaded(pos)
        dataToLoad?.let {
            loadData(it, pos)
            dataToLoad = null
        }
        refreshConnections(world, blockPos)
        setChanged()
        val tag = CompoundTag()
        saveData(tag, pos)
        ClockworkPackets.sendToNear(world, BlockPos.containing(world.toWorldCoordinates(blockPos)), 30, KNodeSyncPacket(pos, tag))
    }

    override fun refreshConnections(level: Level, blockPos: BlockPos) {
        val rear = blockState.getValue(BlockStateProperties.FACING).opposite
        val neighbor = blockPos.relative(rear)
        if (!level.hasChunkAt(neighbor)) return
        val port = level.getBlockState(neighbor).block as? INodeBlock
        val other = if (port?.canConnectTo(neighbor, blockPos, rear.opposite, level) == true)
            ClockworkUtils.getDuctNodePos(neighbor, level) else null
        val network = ClockworkMod.getKelvin(level)
        val self = getDuctNodePosition()
        rearNode?.takeIf { it != other }?.let {
            if (network.getEdgeBetween(self, it) !== pump) network.removeEdge(self, it)
        }
        rearNode = other
        if (other != null && network.getNodeAt(self) != null && network.getNodeAt(other) != null &&
            network.getEdgeBetween(self, other) == null) updateConnection(level, blockPos, rear)
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        val link = ropeId?.let { WandLinks.get(world).links[it] }?.takeIf { !it.suspended && it.a.pos == blockPos }
        val rpm = if (isOverStressed || !getSpeed().isFinite()) 0.0 else abs(getSpeed().toDouble())
        val anchor = link?.b
        if (link != null && anchor != null && anchor.entityId == null && world.hasChunkAt(anchor.pos) &&
            world.getBlockState(anchor.pos).block is INodeBlock) {
            val other = ClockworkUtils.getDuctNodePos(anchor.pos, world)
            updatePump(world, other, rpm, maxOf(link.length, link.previousDistance ?: 0.0))
        } else {
            clearPump()
        }

        val edge = pump
        if (edge != null) {
            if (!pumping || world.gameTime % 5L == 0L) updateReadout("", edge.currentFlowRate / 20.0, true)
            return
        }
        val target = anchor?.entity(world)?.takeIf { it.isAlive }
        val recipe = target?.let { recipes.getRecipeFor(SiphoningRecipe.Input(it.type), world).orElse(null) }
        val mass = if (target != null && recipe != null && rpm > 0.0) siphon(world, target, recipe, rpm) else 0.0
        updateReadout(recipe?.gas?.name.orEmpty(), mass, false)
    }

    private fun siphon(world: ServerLevel, target: Entity, recipe: SiphoningRecipe, rpm: Double): Double {
        val network = ClockworkMod.getKelvin(world)
        val pos = getDuctNodePosition()
        if (dataToLoad != null || network.getNodeAt(pos) == null) return 0.0
        val config = ClockworkConfig.SERVER.siphonHarpoon
        val mass = recipe.massPerTick(rpm)
        val added = mass > 0.0 && network.addGasAtTemperature(pos, recipe.gas, mass, config.gasTemperature)
        val damage = (rpm - config.safeRpm) * config.damagePerExcessRpm
        val damaging = damage > 0.0 && damage.isFinite()
        if (damaging) tickDamage(world, target, damage, config.damageIntervalTicks)
        if (added || damaging) setChanged()
        return if (added) mass else 0.0
    }

    private fun tickDamage(world: ServerLevel, target: Entity, damage: Double, interval: Int) {
        if (damageRopeId != ropeId) {
            damageRopeId = ropeId
            damageTicks = 0
            pendingDamage = 0.0
        }
        pendingDamage = (pendingDamage + damage).coerceAtMost(Float.MAX_VALUE.toDouble())
        damageTicks++
        if (damageTicks < interval) return
        val amount = pendingDamage.toFloat()
        pendingDamage = 0.0
        damageTicks = 0
        val source = ClockworkDamageTypes.siphon(world.registryAccess())
        if (target is EnderDragon) target.hurt(target.head, source, amount) else target.hurt(source, amount)
    }

    private fun updateReadout(gas: String, rate: Double, fromPump: Boolean) {
        if (gas == harvestedGas && rate == kgPerTick && fromPump == pumping) return
        harvestedGas = gas
        kgPerTick = rate
        pumping = fromPump
        sendData()
    }

    private fun updatePump(world: ServerLevel, other: DuctNodePos, rpm: Double, length: Double) {
        val network = ClockworkMod.getKelvin(world)
        val self = getDuctNodePosition()
        if (other == self || network.getNodeAt(self) == null || network.getNodeAt(other) == null || other in network.unloadedNodes) {
            clearPump()
            return
        }
        val existing = network.getEdgeBetween(other, self)
        if (pump !== existing) clearPump()
        val edge = pump ?: run {
            if (existing != null) return
            configureMetalDuctEdge(PumpDuctEdge(other, self, target = self)).also {
                network.addEdge(other, self, it)
                pump = it
            }
        }
        edge.pumpPressure = rpm * ClockworkConfig.SERVER.siphonHarpoon.pumpPressurePerRpm
        edge.length = length.coerceAtLeast(0.5)
    }

    private fun clearPump() {
        val edge = pump ?: return
        val network = ClockworkMod.getKelvin(level)
        if (network.getEdgeBetween(edge.nodeA, edge.nodeB) === edge) network.removeEdge(edge.nodeA, edge.nodeB)
        pump = null
    }

    override fun release() {
        clearPump()
        super.release()
    }

    override fun invalidate() {
        clearPump()
        super.invalidate()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (clientPacket) {
            harvestedGas = tag.getString("HarvestGas")
            kgPerTick = tag.getDouble("GasPerTick")
            pumping = tag.getBoolean("Pumping")
        } else {
            readNodeData(level, tag)
            damageRopeId = ropeId
            damageTicks = tag.getInt("SiphonDamageTicks").coerceIn(0, Int.MAX_VALUE - 1)
            pendingDamage = tag.getDouble("PendingSiphonDamage").takeIf { it in 0.0..Float.MAX_VALUE.toDouble() } ?: 0.0
        }
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        if (clientPacket) {
            tag.putString("HarvestGas", harvestedGas)
            tag.putDouble("GasPerTick", kgPerTick)
            tag.putBoolean("Pumping", pumping)
        } else {
            writeNodeData(level, tag)
            if (damageRopeId == ropeId && damageTicks > 0) {
                tag.putInt("SiphonDamageTicks", damageTicks)
                tag.putDouble("PendingSiphonDamage", pendingDamage)
            }
        }
    }

    override fun addAttachedTooltip(tooltip: MutableList<Component>, length: Double) {
        val rate = ClockworkLang.number(kgPerTick * 1000.0)
        val line = when {
            pumping -> ClockworkLang.translate("gui.siphon_harpoon.pump_flow", rate)
            harvestedGas.isNotEmpty() -> ClockworkLang.translate("gui.siphon_harpoon.harvest", harvestedGas, rate)
            else -> ClockworkLang.translate("gui.siphon_harpoon.no_harvest")
        }
        line.style(ChatFormatting.AQUA).forGoggles(tooltip, 1)
        ClockworkLang.translate("gui.siphon_harpoon.hose_length", ClockworkLang.number(length))
            .style(ChatFormatting.GRAY).forGoggles(tooltip, 1)
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        super<HarpoonBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
        heatableGoggleTooltip(tooltip, isPlayerSneaking)
        return true
    }
}
