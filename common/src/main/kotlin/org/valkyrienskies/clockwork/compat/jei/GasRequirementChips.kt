package org.valkyrienskies.clockwork.compat.jei

import com.google.gson.JsonElement
import mezz.jei.api.gui.builder.ITooltipBuilder
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.clockwork.util.gui.DuctUnits.PressureUnit
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.recipe.GasReactionRequirement
import org.valkyrienskies.kelvin.impl.recipe.DefaultKelvinRequirements
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.roundToInt

/**
 * Renders Kelvin gas recipe requirements as compact Create-style recessed chips
 * with unit-aware formatting (matching Engineer's Goggles) and detailed tooltips.
 */
object GasRequirementChips {
    const val CHIP_HEIGHT = 15
    const val ROW_GAP = 3
    const val ROW_STEP = CHIP_HEIGHT + ROW_GAP
    private const val CHIP_GAP = 4
    private const val PAD_LEFT = 4
    private const val PAD_RIGHT = 4
    private const val ICON_TEXT_GAP = 3
    private const val ICON_SIZE = 9
    private const val INHIBIT_ICON_SIZE = 10

    private const val BORDER_DARK = 0xFF373737.toInt()
    private const val BORDER_LIGHT = 0xFFFFFFFF.toInt()
    private const val BORDER_CORNER = 0xFF8B8B8B.toInt()
    private const val FILL_BG = 0xFF555555.toInt()
    private const val FILL_HOVER = 0xFF636363.toInt()

    private const val COLOR_TEMP_WARM = 0xFFFBBF24.toInt()
    private const val COLOR_TEMP_HOT = 0xFFFB923C.toInt()
    private const val COLOR_TEMP_COOL = 0xFF7DD3FC.toInt()
    private const val COLOR_PRESSURE = 0xFF93C5FD.toInt()
    private const val COLOR_INHIBIT = 0xFFFCA5A5.toInt()
    private const val COLOR_DEFAULT = 0xFFFFFFFF.toInt()

    private const val ICON_V = 56
    private const val TEMP_WARM_U = 176
    private const val TEMP_HOT_U = 186
    private const val TEMP_COOL_U = 196
    private const val PRESSURE_U = 206
    private const val INHIBIT_OVERLAY_U = 216
    private const val GENERIC_INFO_U = 228

    private val TEXTURE: ResourceLocation by lazy {
        ResourceLocation(ClockworkMod.MOD_ID, "textures/gui/jei_widgets.png")
    }

    sealed class ChipIcon {
        data class Sprite(val u: Int, val v: Int = ICON_V, val size: Int = ICON_SIZE) : ChipIcon()
        data class InhibitedGas(val gasIcon: ResourceLocation) : ChipIcon()
    }

    data class Chip(
        val icon: ChipIcon,
        val label: Component,
        val textColor: Int,
        val tooltipLines: List<Component>,
    ) {
        val iconWidth: Int
            get() = when (icon) {
                is ChipIcon.Sprite -> icon.size
                is ChipIcon.InhibitedGas -> INHIBIT_ICON_SIZE
            }
    }

    data class PlacedChip(
        val chip: Chip,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    ) {
        fun contains(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
        }
    }

    fun buildChips(
        requirements: Map<GasReactionRequirement, JsonElement>?,
        isCombustionReaction: Boolean = false,
    ): List<Chip> {
        if (requirements.isNullOrEmpty()) return emptyList()

        val chips = ArrayList<Chip>()

        var minTemp: Double? = null
        var maxTemp: Double? = null
        var minPressure: Double? = null
        var maxPressure: Double? = null
        val extras = ArrayList<Pair<GasReactionRequirement, JsonElement>>()

        for ((req, elem) in requirements) {
            when (req.resourceLocation) {
                DefaultKelvinRequirements.minTemperature.resourceLocation -> minTemp = elem.asDouble
                DefaultKelvinRequirements.maxTemperature.resourceLocation -> maxTemp = elem.asDouble
                DefaultKelvinRequirements.minPressure.resourceLocation -> minPressure = elem.asDouble
                DefaultKelvinRequirements.maxPressure.resourceLocation -> maxPressure = elem.asDouble
                else -> extras.add(req to elem)
            }
        }

        if (minTemp != null || maxTemp != null) {
            chips.add(buildTemperatureChip(minTemp, maxTemp, isCombustionReaction))
        }
        if (minPressure != null || maxPressure != null) {
            chips.add(buildPressureChip(minPressure, maxPressure))
        }

        for ((req, elem) in extras) {
            if (req.resourceLocation == DefaultKelvinRequirements.inhibitedBy.resourceLocation) {
                chips.add(buildInhibitedByChip(elem))
            } else {
                val text = req.get_text(elem)
                chips.add(
                    Chip(
                        icon = ChipIcon.Sprite(GENERIC_INFO_U),
                        label = text,
                        textColor = COLOR_DEFAULT,
                        tooltipLines = listOf(text),
                    )
                )
            }
        }

        return chips
    }

    private fun buildTemperatureChip(minK: Double?, maxK: Double?, isCombustionReaction: Boolean): Chip {
        return when {
            minK != null && maxK != null -> {
                val minComp = formatTemp(minK)
                val maxComp = formatTemp(maxK)
                val hot = minK >= 700.0
                Chip(
                    icon = ChipIcon.Sprite(if (hot) TEMP_HOT_U else TEMP_WARM_U),
                    label = Component.empty().append(minComp).append("–").append(maxComp),
                    textColor = if (hot) COLOR_TEMP_HOT else COLOR_TEMP_WARM,
                    tooltipLines = listOf(
                        ClockworkLang.translate("recipe.gas_requirement.temp_range.title")
                            .style(ChatFormatting.GOLD).component(),
                        ClockworkLang.translate("recipe.gas_requirement.temp_range.desc", minComp, maxComp)
                            .style(ChatFormatting.WHITE).component(),
                    ),
                )
            }
            minK != null -> {
                val minComp = formatTemp(minK)
                val hot = minK >= 700.0
                val titleKey = if (isCombustionReaction) "recipe.gas_requirement.ignition_temp.title" else "recipe.gas_requirement.min_temp.title"
                Chip(
                    icon = ChipIcon.Sprite(if (hot) TEMP_HOT_U else TEMP_WARM_U),
                    label = Component.literal("≥ ").append(minComp),
                    textColor = if (hot) COLOR_TEMP_HOT else COLOR_TEMP_WARM,
                    tooltipLines = listOf(
                        ClockworkLang.translate(titleKey).style(ChatFormatting.GOLD).component(),
                        ClockworkLang.translate("recipe.gas_requirement.min_temp.desc", minComp)
                            .style(ChatFormatting.WHITE).component(),
                    ),
                )
            }
            else -> {
                val maxVal = maxK!!
                val maxComp = formatTemp(maxVal)
                Chip(
                    icon = ChipIcon.Sprite(TEMP_COOL_U),
                    label = Component.literal("≤ ").append(maxComp),
                    textColor = COLOR_TEMP_COOL,
                    tooltipLines = listOf(
                        ClockworkLang.translate("recipe.gas_requirement.max_temp.title")
                            .style(ChatFormatting.AQUA).component(),
                        ClockworkLang.translate("recipe.gas_requirement.max_temp.desc", maxComp)
                            .style(ChatFormatting.WHITE).component(),
                    ),
                )
            }
        }
    }

    private fun buildPressureChip(minPa: Double?, maxPa: Double?): Chip {
        return when {
            minPa != null && maxPa != null -> {
                val minComp = formatPressure(minPa)
                val maxComp = formatPressure(maxPa)
                Chip(
                    icon = ChipIcon.Sprite(PRESSURE_U),
                    label = Component.empty().append(minComp).append("–").append(maxComp),
                    textColor = COLOR_PRESSURE,
                    tooltipLines = listOf(
                        ClockworkLang.translate("recipe.gas_requirement.pressure_range.title")
                            .style(ChatFormatting.BLUE).component(),
                        ClockworkLang.translate("recipe.gas_requirement.pressure_range.desc", minComp, maxComp)
                            .style(ChatFormatting.WHITE).component(),
                    ),
                )
            }
            minPa != null -> {
                val minComp = formatPressure(minPa)
                Chip(
                    icon = ChipIcon.Sprite(PRESSURE_U),
                    label = Component.literal("≥ ").append(minComp),
                    textColor = COLOR_PRESSURE,
                    tooltipLines = listOf(
                        ClockworkLang.translate("recipe.gas_requirement.min_pressure.title")
                            .style(ChatFormatting.BLUE).component(),
                        ClockworkLang.translate("recipe.gas_requirement.min_pressure.desc", minComp)
                            .style(ChatFormatting.WHITE).component(),
                    ),
                )
            }
            else -> {
                val maxVal = maxPa!!
                val maxComp = formatPressure(maxVal)
                Chip(
                    icon = ChipIcon.Sprite(PRESSURE_U),
                    label = Component.literal("≤ ").append(maxComp),
                    textColor = COLOR_PRESSURE,
                    tooltipLines = listOf(
                        ClockworkLang.translate("recipe.gas_requirement.max_pressure.title")
                            .style(ChatFormatting.BLUE).component(),
                        ClockworkLang.translate("recipe.gas_requirement.max_pressure.desc", maxComp)
                            .style(ChatFormatting.WHITE).component(),
                    ),
                )
            }
        }
    }

    private fun buildInhibitedByChip(elem: JsonElement): Chip {
        val obj = elem.asJsonObject
        val gasIdStr = obj["gas"].asString
        val ratio = obj["ratio"].asDouble
        val gasType = GasTypeRegistry.getGasType(ResourceLocation.of(gasIdStr, ':'))
        val gasName = gasType?.name ?: gasIdStr
        val gasIcon = gasType?.iconLocation ?: GasType.PLACEHOLDER_ICON
        val pct = (ratio * 100.0).roundToInt()

        val label = if (pct <= 0) {
            ClockworkLang.translate("recipe.gas_requirement.inhibited_by.none", gasName).component()
        } else {
            Component.literal("< $pct%")
        }

        val descLine = if (pct <= 0) {
            ClockworkLang.translate("recipe.gas_requirement.inhibited_by.desc_none", gasName)
                .style(ChatFormatting.WHITE).component()
        } else {
            ClockworkLang.translate("recipe.gas_requirement.inhibited_by.desc", gasName, "$pct%")
                .style(ChatFormatting.WHITE).component()
        }

        return Chip(
            icon = ChipIcon.InhibitedGas(gasIcon),
            label = label,
            textColor = COLOR_INHIBIT,
            tooltipLines = listOf(
                ClockworkLang.translate("recipe.gas_requirement.inhibited_by.title", gasName)
                    .style(ChatFormatting.RED).component(),
                descLine,
            ),
        )
    }

    private fun formatTemp(kelvin: Double): Component {
        return DuctTextUtil.translateTemperature(ClockworkLang.builder(), kelvin, true).component()
    }

    private fun formatPressure(pascals: Double): Component {
        val doSimplify = ClockworkConfig.CLIENT.ducts.simplifyDisplayUnits
        val targetUnit = ClockworkConfig.CLIENT.ducts.pressureDisplayUnit
        val converted = PressureUnit.BASE.convertTo(pascals, targetUnit)
        val (rawValue, unitKey) = targetUnit.display(converted, doSimplify)
        val cleanValue = rawValue.removeSuffix(".0")
        return ClockworkLang.builder().text(cleanValue).space().translate(unitKey).component()
    }

    fun layout(
        chips: List<Chip>,
        centerX: Int,
        startY: Int,
        maxWidth: Int = 169,
        font: Font = Minecraft.getInstance().font,
    ): List<PlacedChip> {
        if (chips.isEmpty()) return emptyList()

        val rows = ArrayList<MutableList<Pair<Chip, Int>>>()
        var currentRow = ArrayList<Pair<Chip, Int>>()
        var currentWidth = 0

        for (chip in chips) {
            val w = (PAD_LEFT + chip.iconWidth + ICON_TEXT_GAP + font.width(chip.label) + PAD_RIGHT)
                .coerceAtMost(maxWidth)
            val needed = if (currentRow.isEmpty()) w else currentWidth + CHIP_GAP + w
            if (currentRow.isNotEmpty() && needed > maxWidth) {
                rows.add(currentRow)
                currentRow = ArrayList()
                currentRow.add(chip to w)
                currentWidth = w
            } else {
                currentRow.add(chip to w)
                currentWidth = needed
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val placed = ArrayList<PlacedChip>(chips.size)
        for ((rowIdx, row) in rows.withIndex()) {
            val rowTotalWidth = row.sumOf { it.second } + CHIP_GAP * (row.size - 1)
            var x = centerX - rowTotalWidth / 2
            val y = startY + rowIdx * ROW_STEP
            for ((chip, w) in row) {
                placed.add(PlacedChip(chip, x, y, w, CHIP_HEIGHT))
                x += w + CHIP_GAP
            }
        }
        return placed
    }

    fun measureHeight(
        requirements: Map<GasReactionRequirement, JsonElement>?,
        isCombustionReaction: Boolean = false,
        maxWidth: Int = 169,
    ): Int {
        val chips = buildChips(requirements, isCombustionReaction)
        if (chips.isEmpty()) return 0
        val placed = layout(chips, 0, 0, maxWidth)
        val maxBottom = placed.maxOf { it.y + it.height }
        return maxBottom + 2
    }

    fun render(
        graphics: GuiGraphics,
        requirements: Map<GasReactionRequirement, JsonElement>?,
        centerX: Int,
        startY: Int,
        mouseX: Double,
        mouseY: Double,
        isCombustionReaction: Boolean = false,
        maxWidth: Int = 169,
    ): List<PlacedChip> {
        val chips = buildChips(requirements, isCombustionReaction)
        if (chips.isEmpty()) return emptyList()

        val font = Minecraft.getInstance().font
        val placed = layout(chips, centerX, startY, maxWidth, font)

        for (item in placed) {
            val hovered = item.contains(mouseX, mouseY)
            drawRecessedChipBox(graphics, item.x, item.y, item.width, item.height, hovered)

            val iconX = item.x + PAD_LEFT
            when (val icon = item.chip.icon) {
                is ChipIcon.Sprite -> {
                    val iconY = item.y + (item.height - icon.size) / 2
                    graphics.blit(TEXTURE, iconX, iconY, 0, icon.u.toFloat(), icon.v.toFloat(), icon.size, icon.size, 256, 256)
                }
                is ChipIcon.InhibitedGas -> {
                    val iconY = item.y + (item.height - INHIBIT_ICON_SIZE) / 2
                    // Render the actual gas texture scaled inside the prohibition circle
                    graphics.blit(icon.gasIcon, iconX + 1, iconY + 1, 8, 8, 0f, 0f, 16, 16, 16, 16)
                    graphics.blit(TEXTURE, iconX, iconY, 0, INHIBIT_OVERLAY_U.toFloat(), ICON_V.toFloat(), INHIBIT_ICON_SIZE, INHIBIT_ICON_SIZE, 256, 256)
                }
            }

            val textX = iconX + item.chip.iconWidth + ICON_TEXT_GAP
            val textY = item.y + (item.height - font.lineHeight) / 2 + 1
            val maxTextW = (item.x + item.width - PAD_RIGHT - textX).coerceAtLeast(8)
            val drawnText = if (font.width(item.chip.label) > maxTextW) {
                Component.literal(font.substrByWidth(item.chip.label, maxTextW).string)
            } else {
                item.chip.label
            }
            graphics.drawString(font, drawnText, textX, textY, item.chip.textColor, true)
        }

        return placed
    }

    fun addTooltip(
        tooltip: ITooltipBuilder,
        requirements: Map<GasReactionRequirement, JsonElement>?,
        centerX: Int,
        startY: Int,
        mouseX: Double,
        mouseY: Double,
        isCombustionReaction: Boolean = false,
        maxWidth: Int = 169,
    ) {
        val chips = buildChips(requirements, isCombustionReaction)
        if (chips.isEmpty()) return
        val placed = layout(chips, centerX, startY, maxWidth)
        val hit = placed.firstOrNull { it.contains(mouseX, mouseY) } ?: return
        tooltip.addAll(hit.chip.tooltipLines)
    }

    private fun drawRecessedChipBox(graphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, hovered: Boolean) {
        // Inner dark fill
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, if (hovered) FILL_HOVER else FILL_BG)
        // Top & left recessed shadow
        graphics.fill(x, y, x + w - 1, y + 1, BORDER_DARK)
        graphics.fill(x, y + 1, x + 1, y + h - 1, BORDER_DARK)
        // Bottom & right recessed highlight
        graphics.fill(x + 1, y + h - 1, x + w, y + h, BORDER_LIGHT)
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, BORDER_LIGHT)
        // Neutral corner pixels (matching Minecraft slot / Style 1 bezel)
        graphics.fill(x + w - 1, y, x + w, y + 1, BORDER_CORNER)
        graphics.fill(x, y + h - 1, x + 1, y + h, BORDER_CORNER)
    }
}
