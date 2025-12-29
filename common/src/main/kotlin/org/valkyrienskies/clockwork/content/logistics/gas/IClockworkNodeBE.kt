package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import dev.architectury.platform.Platform
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import kotlin.apply

interface IClockworkNodeBE: INodeBlockEntity, IHaveGoggleInformation {

    fun heatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        ClockworkLang.translate("gui.ductInfo.title").forGoggles(tooltip)

        val pos = this.getDuctNodePosition()
        val kelvin = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()
        val blockStats = Minecraft.getInstance().level?.getBlockState(pos.toMinecraft())?.block as? IHaveDuctStats

        var found = false

        kelvin.nodeInfo[pos]?.totalVolume?.let { volume ->

            found = true
            ClockworkLang.builder().apply {
                add(ClockworkLang.translate("gui.ductInfo.volume").add(ClockworkLang.text(": ")).style(ChatFormatting.GREEN))
                add(DuctTextUtil.translateVolume(ClockworkLang.builder(), volume, true).style(ChatFormatting.GREEN))
                space()
                forGoggles(tooltip, 1)
            }
        }
        kelvin.getTemperatureAt(pos).let { temp ->
            found = true
            ClockworkLang.builder().apply {
                add(ClockworkLang.translate("gui.ductInfo.temperature").add(ClockworkLang.text(": ")).style(ChatFormatting.GOLD))
                add(DuctTextUtil.translateTemperature(ClockworkLang.builder(), temp, true).style(ChatFormatting.GOLD))

                if (isPlayerSneaking)
                blockStats?.getMaximumTemperature()?.let { max ->
                    add(ClockworkLang.text(" / ")
                        .add(DuctTextUtil.translateTemperature(ClockworkLang.builder(), max, true))
                        .style(ChatFormatting.DARK_GRAY)
                    )
                }
                space()

                forGoggles(tooltip, 0)
            }
        }
        kelvin.getPressureAt(pos).takeIf { it > 0.0 }?.let { pressure ->
            found = true
            ClockworkLang.builder().apply {
                add(ClockworkLang.translate("gui.ductInfo.pressure").add(ClockworkLang.text(": ")).style(ChatFormatting.BLUE))
                add(DuctTextUtil.translatePressure(ClockworkLang.builder(), pressure, true).style(ChatFormatting.BLUE))

                if (isPlayerSneaking)
                    blockStats?.getMaximumPressure()?.let { max ->
                        add(ClockworkLang.text(" / ")
                            .add(DuctTextUtil.translatePressure(ClockworkLang.builder(), max, true))
                            .style(ChatFormatting.DARK_GRAY)
                        )
                    }
                space()

                forGoggles(tooltip, 0)
            }
        }
        if (isPlayerSneaking)
        kelvin.getHeatEnergy(pos).takeIf { it > 0.0 }?.let { energy ->
            found = true
            ClockworkLang.builder().apply {
                add(ClockworkLang.translate("gui.ductInfo.energy").style(ChatFormatting.RED))
                add(DuctTextUtil.translateEnergy(ClockworkLang.builder(), energy, true).style(ChatFormatting.RED))
                space()

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

                    val component = Component.empty().apply {
                        append(Component.literal(ClockworkGasses.getDisplayCharacterCode(gas))
                            .withStyle { it.withFont(ClockworkGasses.ICON_FONT_LOCATION) })
                        if (isPlayerSneaking) {
                            append(Component.literal(" ${gas.name} ").withStyle(ChatFormatting.GRAY))
                        } else {
                            append(Component.literal(" "))
                        }
                        append(DuctTextUtil.translateMass(ClockworkLang.builder(), amount, true).component())
                    }
                    finishedComponents.add(component)
                }
            if (finishedComponents.isEmpty()) return@let

            ClockworkLang.translate("gui.ductInfo.title.contents").forGoggles(tooltip)

            val rows = mutableListOf<Pair<Component, Component>>()
            for (num in finishedComponents.indices step 2) {
                if (num + 1 < finishedComponents.size) {
                    rows.add(Pair(finishedComponents[num], finishedComponents[num + 1]))
                } else {
                    rows.add(Pair(finishedComponents[num], Component.empty()))
                }
            }
            for (row in rows) {
                val rowComponent = Component.empty()
                    .append(row.first)
                    .append(Component.literal("  "))
                    .append(row.second)
                ClockworkLang.builder().add(rowComponent).forGoggles(tooltip, 1)
            }

            if (isPlayerSneaking && finishedComponents.size > 1)
                ClockworkLang.translate("gui.ductInfo.mass_total").add(
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
