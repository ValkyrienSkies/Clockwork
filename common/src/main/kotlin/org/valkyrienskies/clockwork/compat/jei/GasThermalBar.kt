package org.valkyrienskies.clockwork.compat.jei

import mezz.jei.api.gui.builder.ITooltipBuilder
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * This is the thermal output bar for reactions and gas crafting jei ui
 */
object GasThermalBar {
    const val TOTAL_SQUARES = 10
    const val SAFE_SQUARES = 8
    const val AMBIENT_TEMP_K = 293.15
    const val BAR_MIN_TEMP_K = 300.0
    const val STANDARD_DUCT_MAX_TEMP_K = 1478.0
    const val COMBUSTOR_MAX_TEMP_K = 1900.0

    const val BAR_HEIGHT = 11
    private const val CELL_WIDTH = 4
    private const val CELL_HEIGHT = 5
    private const val CELL_STEP = 5
    private const val REDLINE_GAP = 3
    private const val ICON_SIZE = 9
    private const val ICON_GAP = 2
    private const val WARN_SIZE = 7
    private const val WARN_GAP = 2
    private const val REDLINE_BAR_WIDTH = 56
    private const val PLAIN_BAR_WIDTH = 53

    private const val CRAFT_LOG_MIN = 3.8  // ~6.3 kJ
    private const val CRAFT_LOG_MAX = 5.35 // ~224 kJ

    private const val EMPTY_REDLINE_V = 16
    private const val EMPTY_PLAIN_V = 28
    private const val HOT_CELLS_V = 40
    private const val COLD_CELLS_V = 46

    private val TEXTURE: ResourceLocation by lazy {
        ResourceLocation(ClockworkMod.MOD_ID, "textures/gui/jei_widgets.png")
    }

    data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int) {
        fun contains(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
        }
    }

    enum class ThermalTier(val langKey: String, val color: ChatFormatting) {
        WARM("recipe.gas_thermal.tier.warm", ChatFormatting.GOLD),
        KINDLED("recipe.gas_thermal.tier.kindled", ChatFormatting.GOLD),
        SEETHING("recipe.gas_thermal.tier.seething", ChatFormatting.YELLOW),
        NEAR_DUCT_LIMIT("recipe.gas_thermal.tier.near_duct_limit", ChatFormatting.RED),
        MELTS_DUCTS("recipe.gas_thermal.tier.melts_ducts", ChatFormatting.RED),
        SLIGHT_HEAT("recipe.gas_thermal.tier.slight_heat", ChatFormatting.GOLD),
        RELEASES_HEAT("recipe.gas_thermal.tier.releases_heat", ChatFormatting.GOLD),
        INTENSE_HEAT("recipe.gas_thermal.tier.intense_heat", ChatFormatting.RED),
        COOLS("recipe.gas_thermal.tier.cools", ChatFormatting.AQUA),
        HEAVY_COOLING("recipe.gas_thermal.tier.heavy_cooling", ChatFormatting.AQUA),
    }

    data class ThermalInfo(
        val squares: Int,
        val energyJoules: Double,
        val burnTempK: Double?,
        val isCold: Boolean,
        val showRedline: Boolean,
        val meltsDuct: Boolean,
        val isCrafting: Boolean,
        val tier: ThermalTier,
    )

    fun mixtureHeatCapacity(gases: Map<GasType, Double>): Double {
        var total = 0.0
        for ((gas, massKg) in gases) {
            if (massKg <= 0.0 || gas.adiabaticIndex <= 0.0) continue
            total += massKg * gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
        }
        return total
    }

    fun fromGasReaction(
        recipe: GasBaseRecipe,
        kindledTempK: Double = safeKindledTemp(),
        seethingTempK: Double = safeSeethingTemp(),
    ): ThermalInfo? {
        val energy = recipe.energy
        if (!energy.isFinite() || abs(energy) < 1e-6) return null

        if (energy < 0.0) {
            val squares = energyToCraftSquares(energy)
            val tier = if (squares >= 7) ThermalTier.HEAVY_COOLING else ThermalTier.COOLS
            return ThermalInfo(
                squares = squares,
                energyJoules = energy,
                burnTempK = null,
                isCold = true,
                showRedline = false,
                meltsDuct = false,
                isCrafting = false,
                tier = tier,
            )
        }

        val inCv = mixtureHeatCapacity(recipe.gasses)
        val outCv = mixtureHeatCapacity(recipe.result)
        if (inCv > 0.0 && outCv > 0.0) {
            val burnTempK = (inCv * AMBIENT_TEMP_K + energy) / outCv
            val meltsDuct = burnTempK > STANDARD_DUCT_MAX_TEMP_K
            val squares = burnTempToSquares(burnTempK)
            val tier = when {
                meltsDuct -> ThermalTier.MELTS_DUCTS
                burnTempK >= STANDARD_DUCT_MAX_TEMP_K - 50.0 -> ThermalTier.NEAR_DUCT_LIMIT
                burnTempK >= seethingTempK -> ThermalTier.SEETHING
                burnTempK >= kindledTempK -> ThermalTier.KINDLED
                else -> ThermalTier.WARM
            }
            return ThermalInfo(
                squares = squares,
                energyJoules = energy,
                burnTempK = burnTempK,
                isCold = false,
                showRedline = true,
                meltsDuct = meltsDuct,
                isCrafting = false,
                tier = tier,
            )
        }

        val squares = energyToCraftSquares(energy)
        return ThermalInfo(
            squares = squares,
            energyJoules = energy,
            burnTempK = null,
            isCold = false,
            showRedline = false,
            meltsDuct = false,
            isCrafting = false,
            tier = craftHeatTier(squares),
        )
    }

    fun fromGasCrafting(recipe: GasCraftingRecipe): ThermalInfo? {
        val gasRecipe = recipe.gasRecipe ?: return null
        val energy = gasRecipe.energy
        if (!energy.isFinite() || abs(energy) < 1e-6) return null

        val isCold = energy < 0.0
        val squares = energyToCraftSquares(energy)
        val tier = if (isCold) {
            if (squares >= 7) ThermalTier.HEAVY_COOLING else ThermalTier.COOLS
        } else {
            craftHeatTier(squares)
        }
        return ThermalInfo(
            squares = squares,
            energyJoules = energy,
            burnTempK = null,
            isCold = isCold,
            showRedline = false,
            meltsDuct = false,
            isCrafting = true,
            tier = tier,
        )
    }

    fun burnTempToSquares(burnTempK: Double): Int {
        if (burnTempK <= STANDARD_DUCT_MAX_TEMP_K) {
            val frac = (burnTempK - BAR_MIN_TEMP_K) / (STANDARD_DUCT_MAX_TEMP_K - BAR_MIN_TEMP_K)
            return (frac * SAFE_SQUARES).roundToInt().coerceIn(1, SAFE_SQUARES)
        }
        val extraSpan = (COMBUSTOR_MAX_TEMP_K - STANDARD_DUCT_MAX_TEMP_K) / 2.0
        val extraStep = ((burnTempK - STANDARD_DUCT_MAX_TEMP_K) / extraSpan).toInt().coerceIn(0, 1)
        return (SAFE_SQUARES + 1 + extraStep).coerceIn(SAFE_SQUARES + 1, TOTAL_SQUARES)
    }

    fun energyToCraftSquares(energyJoules: Double): Int {
        val magnitude = abs(energyJoules).coerceAtLeast(1.0)
        val logVal = log10(magnitude)
        val t = ((logVal - CRAFT_LOG_MIN) / (CRAFT_LOG_MAX - CRAFT_LOG_MIN)).coerceIn(0.0, 1.0)
        return (1.0 + t * (TOTAL_SQUARES - 1)).roundToInt().coerceIn(1, TOTAL_SQUARES)
    }

    private fun craftHeatTier(squares: Int): ThermalTier = when {
        squares >= 7 -> ThermalTier.INTENSE_HEAT
        squares >= 4 -> ThermalTier.RELEASES_HEAT
        else -> ThermalTier.SLIGHT_HEAT
    }

    private fun safeKindledTemp(): Double = runCatching {
        ClockworkConfig.SERVER.gasHeater.heaterKindledTemp.toDouble()
    }.getOrDefault(800.0)

    private fun safeSeethingTemp(): Double = runCatching {
        ClockworkConfig.SERVER.gasHeater.heaterSeethingTemp.toDouble()
    }.getOrDefault(1300.0)

    fun totalWidth(info: ThermalInfo): Int {
        val barWidth = if (info.showRedline) REDLINE_BAR_WIDTH else PLAIN_BAR_WIDTH
        val warnExtra = if (info.meltsDuct) WARN_GAP + WARN_SIZE else 0
        return ICON_SIZE + ICON_GAP + barWidth + warnExtra
    }

    fun boundsCentered(centerX: Int, y: Int, info: ThermalInfo): Bounds {
        val barWidth = if (info.showRedline) REDLINE_BAR_WIDTH else PLAIN_BAR_WIDTH
        val barX = centerX - barWidth / 2
        val leftX = barX - ICON_GAP - ICON_SIZE
        val rightX = barX + barWidth + if (info.meltsDuct) WARN_GAP + WARN_SIZE else 0
        return Bounds(leftX, y, rightX - leftX, BAR_HEIGHT)
    }

    fun render(
        graphics: GuiGraphics,
        centerX: Int,
        y: Int,
        info: ThermalInfo,
    ): Bounds {
        val bounds = boundsCentered(centerX, y, info)
        val barWidth = if (info.showRedline) REDLINE_BAR_WIDTH else PLAIN_BAR_WIDTH
        val barX = centerX - barWidth / 2
        val iconX = barX - ICON_GAP - ICON_SIZE

        // 1. Left flame / frost icon (9x9)
        val iconU = when {
            info.isCold -> 196
            info.meltsDuct -> 186
            else -> 176
        }
        graphics.blit(TEXTURE, iconX, y + 1, 0, iconU.toFloat(), 0f, ICON_SIZE, ICON_SIZE, 256, 256)

        // 2. Empty bar frame with baked checkerboard + optional redline
        val emptyV = if (info.showRedline) EMPTY_REDLINE_V else EMPTY_PLAIN_V
        graphics.blit(TEXTURE, barX, y, 0, 176f, emptyV.toFloat(), barWidth, BAR_HEIGHT, 256, 256)

        // 3. Lit cells (1..info.squares)
        val cellV = if (info.isCold) COLD_CELLS_V else HOT_CELLS_V
        val litCount = info.squares.coerceIn(0, TOTAL_SQUARES)
        for (i in 0 until litCount) {
            val extraGap = if (info.showRedline && i >= SAFE_SQUARES) REDLINE_GAP else 0
            val dstX = barX + 2 + i * CELL_STEP + extraGap
            val srcU = 176 + i * CELL_STEP
            graphics.blit(TEXTURE, dstX, y + 3, 0, srcU.toFloat(), cellV.toFloat(), CELL_WIDTH, CELL_HEIGHT, 256, 256)
        }

        // 4. Right melt warning [!] badge when exceeding standard duct limit
        if (info.meltsDuct) {
            val warnX = barX + barWidth + WARN_GAP
            graphics.blit(TEXTURE, warnX, y + 2, 0, 206f, 1f, WARN_SIZE, WARN_SIZE, 256, 256)
        }

        return bounds
    }

    fun addTooltip(tooltip: ITooltipBuilder, info: ThermalInfo) {
        tooltip.add(
            ClockworkLang.translate(info.tier.langKey)
                .style(info.tier.color)
                .component()
        )

        val burnTemp = info.burnTempK
        if (burnTemp != null) {
            val formattedTemp = DuctTextUtil.translateTemperature(ClockworkLang.builder(), burnTemp, true).component()
            tooltip.add(
                ClockworkLang.translate("recipe.gas_thermal.burn_temp", formattedTemp)
                    .style(ChatFormatting.WHITE)
                    .component()
            )
            if (info.meltsDuct) {
                val limitTemp = DuctTextUtil.translateTemperature(ClockworkLang.builder(), STANDARD_DUCT_MAX_TEMP_K, true).component()
                tooltip.add(
                    ClockworkLang.translate("recipe.gas_thermal.melts_ducts", limitTemp)
                        .style(ChatFormatting.RED)
                        .component()
                )
            } else if (burnTemp >= safeSeethingTemp()) {
                tooltip.add(
                    ClockworkLang.translate("recipe.gas_thermal.reaches_seething")
                        .style(ChatFormatting.GRAY)
                        .component()
                )
            } else if (burnTemp >= safeKindledTemp()) {
                tooltip.add(
                    ClockworkLang.translate("recipe.gas_thermal.reaches_kindled")
                        .style(ChatFormatting.GRAY)
                        .component()
                )
            }
        } else if (info.isCold) {
            tooltip.add(
                ClockworkLang.translate("recipe.gas_thermal.endothermic_hint")
                    .style(ChatFormatting.AQUA)
                    .component()
            )
        } else if (info.isCrafting) {
            tooltip.add(
                ClockworkLang.translate("recipe.gas_thermal.exothermic_craft_hint")
                    .style(ChatFormatting.GRAY)
                    .component()
            )
        }

        val sign = if (info.energyJoules >= 0.0) "+" else "-"
        val energyText = Component.literal(sign).append(
            DuctTextUtil.translateEnergy(ClockworkLang.builder(), abs(info.energyJoules), true).component()
        )
        val energyKey = if (info.isCrafting) "recipe.gas_thermal.energy_per_craft" else "recipe.gas_thermal.energy_per_batch"
        tooltip.add(
            ClockworkLang.translate(energyKey, energyText)
                .style(ChatFormatting.DARK_GRAY)
                .component()
        )
    }
}
