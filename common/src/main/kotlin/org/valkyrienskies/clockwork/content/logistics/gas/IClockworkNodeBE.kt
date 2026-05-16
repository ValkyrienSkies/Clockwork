package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import dev.architectury.platform.Platform
import net.createmod.ponder.api.level.PonderLevel
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil.gasComponent
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import kotlin.apply

interface IClockworkNodeBE: INodeBlockEntity, IHaveGoggleInformation {


    fun heatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        ClockworkLang.translate("gui.ductInfo.title").forGoggles(tooltip)

        val pos = this.getDuctNodePosition()
        val beLevel = (this as? BlockEntity)?.level
        val kelvin = if (beLevel is PonderLevel) ClockworkMod.getKelvin(beLevel)
            else if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin()
            else ClockworkModClient.getKelvin()
        val blockStats = Minecraft.getInstance().level?.getBlockState(pos.toMinecraft())?.block as? IHaveDuctStats

        var found = false

        kelvin.nodeInfo[pos]?.totalVolume?.let { volume ->
            found = true
            ClockworkLang.builder().apply {
                add(ClockworkLang.translate(
                    "gui.ductInfo.volume",
                DuctTextUtil.translateVolume(ClockworkLang.builder(), volume, true)
                ).style(ChatFormatting.GREEN))
                forGoggles(tooltip, 0)
            }
        }
        kelvin.getTemperatureAt(pos).let { temp ->
            found = true
            ClockworkLang.builder().apply {
                val max = blockStats?.getMaximumTemperature()
                val critical = if (max != null) temp > max * ClockworkConfig.CLIENT.maxTemperatureWarning else false
                val indents = if (critical) -2 else 0

                if (critical) text("!! ")
                add(ClockworkLang.translate(
                    "gui.ductInfo.temperature",
                    DuctTextUtil.translateTemperature(ClockworkLang.builder(), temp, true)
                ))
                style(ChatFormatting.GOLD)

                if (isPlayerSneaking && max != null) {
                    add(ClockworkLang.translate(
                        "gui.ductInfo.out_of",
                        DuctTextUtil.translateTemperature(ClockworkLang.builder(), max, true)
                    ).style(ChatFormatting.DARK_GRAY))
                }
                forGoggles(tooltip, indents)
            }
        }
        kelvin.getPressureAt(pos).takeIf { it > 0.0 }?.let { pressure ->
            found = true
            ClockworkLang.builder().apply {
                val max = blockStats?.getMaximumPressure()
                val critical = if (max != null) pressure > max * ClockworkConfig.CLIENT.maxPressureWarning else false
                val indents = if (critical) -2 else 0

                if (critical) text("!! ")
                add(ClockworkLang.translate(
                    "gui.ductInfo.pressure",
                    DuctTextUtil.translatePressure(ClockworkLang.builder(), pressure, true)
                ))
                style(ChatFormatting.BLUE)

                if (isPlayerSneaking)
                    if (max != null) {
                        add(ClockworkLang.translate(
                            "gui.ductInfo.out_of",
                            DuctTextUtil.translatePressure(ClockworkLang.builder(), max, true)
                        ).style(ChatFormatting.DARK_GRAY))
                    }
                forGoggles(tooltip, indents)
            }
        }
        if (isPlayerSneaking)
        kelvin.getHeatEnergy(pos).takeIf { it > 0.0 }?.let { energy ->
            found = true
            ClockworkLang.builder().apply {
                add(ClockworkLang.translate(
                    "gui.ductInfo.energy",
                    DuctTextUtil.translateEnergy(ClockworkLang.builder(), energy, true)
                ).style(ChatFormatting.RED))
                forGoggles(tooltip, 0)
            }
        }
        kelvin.getGasMassAt(pos).takeIf { it.isNotEmpty() }?.let { gasMap ->
            found = true

            var totalGasMass = 0.0
            val finishedComponents = mutableListOf<Component>()
            gasMap.entries
                .sortedByDescending { it.value }
                .mapNotNull { (gas, amount) ->
                    if (amount <= 0.0) return@mapNotNull null
                    totalGasMass += amount
                    finishedComponents.add(gasComponent(gas, amount, isPlayerSneaking))
                }
            if (finishedComponents.isEmpty()) return@let

            ClockworkLang.translate("gui.ductInfo.title.contents").forGoggles(tooltip)

            for (row in finishedComponents.chunked(2)) {
                ClockworkLang.builder().add(
                    ClockworkLang.builder().apply {
                        add(row[0])
                        if (row.size > 1) {
                            text("  ")
                            add(row[1])
                        }
                    }
                ).forGoggles(tooltip, 1)
            }

            if (isPlayerSneaking && finishedComponents.size > 1)
                ClockworkLang.translate(
                    "gui.ductInfo.mass_total",
                    DuctTextUtil.translateMass(ClockworkLang.builder(), totalGasMass, true)
                ).style(ChatFormatting.GRAY).forGoggles(tooltip, 1)

            // can we pass leak ratio to client? looks useful
        }

        if (!found) {
            ClockworkLang.translate("gui.ductInfo.notFound").style(ChatFormatting.GRAY).forGoggles(tooltip, 1)
        }
        return found
    }
}
