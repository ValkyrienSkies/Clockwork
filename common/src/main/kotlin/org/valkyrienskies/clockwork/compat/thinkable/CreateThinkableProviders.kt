package org.valkyrienskies.clockwork.compat.thinkable

import com.simibubi.create.AllBlockEntityTypes
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot
import com.simibubi.create.content.redstone.DirectedDirectionalBlock
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.api.thinkable.Thinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import org.valkyrienskies.clockwork.api.thinkable.ThinkableRegistry
import org.valkyrienskies.clockwork.platform.CreateThinkableMethods

object CreateThinkableProviders {
    fun register(registry: ThinkableRegistry) {
        registry.registerBlockEntity(AllBlockEntityTypes.SPEEDOMETER.get()) { _, be ->
            output("speed", "absolute_speed") { if (it == "speed") be.speed.toDouble() else kotlin.math.abs(be.speed.toDouble()) }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.STRESSOMETER.get()) { _, be ->
            output("stress_percent", "stress", "capacity", "remaining_stress") {
                val stress = be.networkStress.toDouble()
                val capacity = be.networkCapacity.toDouble()
                when (it) {
                    "stress" -> stress
                    "capacity" -> capacity
                    "remaining_stress" -> capacity - stress
                    else -> if (capacity == 0.0) 0.0 else 100.0 * stress / capacity
                }
            }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.THRESHOLD_SWITCH.get()) { _, be ->
            output("fill_percent", "current_amount", "minimum_amount", "maximum_amount") {
                if (be.currentLevel == -1) null else when (it) {
                    "current_amount" -> be.currentLevel.toDouble()
                    "minimum_amount" -> be.currentMinLevel.toDouble()
                    "maximum_amount" -> be.currentMaxLevel.toDouble()
                    else -> if (be.currentMaxLevel <= be.currentMinLevel) null else
                        100.0 * (be.currentLevel.toDouble() - be.currentMinLevel) / (be.currentMaxLevel.toDouble() - be.currentMinLevel)
                }
            }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.SMART_OBSERVER.get()) { context, be ->
            output("items", "fluid") {
                if (!context.level().hasChunkAt(context.pos().relative(DirectedDirectionalBlock.getTargetDirection(be.blockState)))) null
                else if (it == "items") CreateThinkableMethods.observedItems(be) else CreateThinkableMethods.observedFluid(be)
            }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.FLUID_TANK.get()) { context, be ->
            output("fluid", "fluid_capacity", "fill_percent", "boiler_heat", "boiler_water", "boiler_size") {
                val controller = if (context.level().hasChunkAt(be.controller)) be.controllerBE else null
                if (controller == null) null else when (it) {
                    "fluid" -> CreateThinkableMethods.tankFluid(controller, false)
                    "fluid_capacity" -> CreateThinkableMethods.tankFluid(controller, true)
                    "fill_percent" -> {
                        val capacity = CreateThinkableMethods.tankFluid(controller, true)
                        if (capacity <= 0.0) null else 100.0 * CreateThinkableMethods.tankFluid(controller, false) / capacity
                    }
                    "boiler_heat" -> controller.boiler.theoreticalHeatLevel.toDouble()
                    "boiler_water" -> controller.boiler.maxHeatLevelForWaterSupply.toDouble()
                    else -> controller.boiler.getMaxHeatLevelForBoilerSize(controller.totalTankSize).toDouble()
                }
            }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.CUCKOO_CLOCK.get()) { context, be ->
            output("day_ticks", "day_hour") {
                val world = context.level() as? ServerLevel
                if (world == null || be.speed == 0f || !world.dimensionType().natural()) null else {
                    val ticks = Math.floorMod(world.dayTime, 24000L).toDouble()
                    if (it == "day_ticks") ticks else (ticks / 1000.0 + 6.0) % 24.0
                }
            }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.NIXIE_TUBE.get()) { _, be ->
            output("number") { be.fullText.string.trim().toDoubleOrNull() }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.ELEVATOR_CONTACT.get()) { _, be ->
            output("floor") { be.lastReportedCurrentFloor.trim().toDoubleOrNull() }
        }
        registry.registerBlockEntity(AllBlockEntityTypes.FACTORY_PANEL.get()) { _, be ->
            val keys = PanelSlot.values().flatMap { slot -> listOf("${slot.name.lowercase()}/stock", "${slot.name.lowercase()}/promised", "${slot.name.lowercase()}/demand") }
            output(*keys.toTypedArray()) { key ->
                val slot = PanelSlot.values().first { key.startsWith(it.name.lowercase() + "/") }
                val panel = be.panels[slot]
                if (panel == null || !panel.isActive) null else when (key.substringAfter('/')) {
                    "stock" -> panel.levelInStorage.toDouble()
                    "promised" -> panel.promised.toDouble()
                    else -> panel.amount.toDouble() * if (panel.upTo) 1 else panel.filter.maxStackSize
                }
            }
        }
        registry.registerBlock(Blocks.TARGET) { context ->
            output("redstone") { context.level().getBlockState(context.pos()).getValue(BlockStateProperties.POWER).toDouble() }
        }
        registry.registerBlockEntity(BlockEntityType.ENCHANTING_TABLE) { context, _ ->
            output("enchant_power") { CreateThinkableMethods.enchantPower(context.level(), context.pos()) }
        }
    }

    private fun output(vararg keys: String, read: (String) -> Double?): Thinkable = object : Thinkable {
        override fun providesThinkable(side: Direction) = true
        override fun thinkableReadings(side: Direction) = keys.map { ThinkableReading(it, name(it)) }
        override fun thinkableValue(side: Direction) = thinkableValue(side, "")
        override fun thinkableValue(side: Direction, reading: String): Double? {
            val key = reading.ifEmpty { keys.first() }
            return if (key in keys) read(key)?.takeIf { it.isFinite() } else null
        }
        override fun thinkablePortName(side: Direction) = Component.translatable("thinkable.vs_clockwork.port.readings")
    }

    private fun name(key: String): Component = if ('/' in key)
        Component.translatable("thinkable.vs_clockwork.reading.panel", Component.translatable("thinkable.vs_clockwork.reading.${key.substringBefore('/')}"),
            Component.translatable("thinkable.vs_clockwork.reading.${key.substringAfter('/')}"))
    else Component.translatable("thinkable.vs_clockwork.reading.$key")
}
