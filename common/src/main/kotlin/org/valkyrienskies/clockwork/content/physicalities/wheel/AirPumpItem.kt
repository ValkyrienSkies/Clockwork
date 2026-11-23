package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.simibubi.create.content.equipment.armor.BacktankUtil
import net.createmod.catnip.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkSounds
import java.util.Locale

enum class AirPumpSetting(val id: String) {
    PRESSURE("pressure"), WIDTH("width"), THICKNESS("thickness");

    val label get() = Component.translatable("block.vs_clockwork.wheel_hub.$id")

    fun range(tire: WheelTire) = when (this) {
        PRESSURE -> minOf(20, value(tire))..320
        WIDTH -> tire.size.minimumWidthPercent / 5..40
        THICKNESS -> 5..tire.size.maximumThicknessPercent / 5
    }

    fun value(tire: WheelTire) = when (this) {
        PRESSURE -> tire.pressureKpa / 5
        WIDTH -> tire.widthPercent / 5
        THICKNESS -> tire.thicknessPercent / 5
    }

    fun readout(tire: WheelTire): Component {
        val measurement = when (this) {
            PRESSURE -> String.format(Locale.ROOT, "%.2f bar", tire.pressureKpa / 100.0)
            WIDTH -> String.format(Locale.ROOT, "%.2f m", tire.width)
            THICKNESS -> String.format(Locale.ROOT, "%.2f m", tire.thickness)
        }
        return Component.translatable("item.vs_clockwork.air_pump.readout", label, measurement)
    }
}

class AirPumpItem(properties: Properties) : Item(properties) {
    override fun isBarVisible(stack: ItemStack) = BacktankUtil.isBarVisible(stack, USES_PER_TANK)
    override fun getBarWidth(stack: ItemStack) = BacktankUtil.getBarWidth(stack, USES_PER_TANK)
    override fun getBarColor(stack: ItemStack) = BacktankUtil.getBarColor(stack, USES_PER_TANK)

    internal fun setting(stack: ItemStack) =
        AirPumpSetting.entries.find { it.id == stack.tag?.getString("Setting") } ?: AirPumpSetting.PRESSURE

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        tooltip.add(setting(stack).label)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!player.isShiftKeyDown) return InteractionResultHolder.pass(stack)
        if (!level.isClientSide) {
            val modes = AirPumpSetting.entries
            val next = modes[(setting(stack).ordinal + 1) % modes.size]
            stack.orCreateTag.putString("Setting", next.id)
            player.displayClientMessage(next.label, true)
            player.cooldowns.addCooldown(this, STROKE_TICKS)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity ?: return InteractionResult.PASS
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild()) return InteractionResult.FAIL
        val tire = hub.tire
        if (tire == null) {
            if (!context.level.isClientSide) player.displayClientMessage(Component.translatable("item.vs_clockwork.air_pump.fit_tire"), true)
            return InteractionResult.FAIL
        }
        val mode = setting(context.itemInHand)
        if (mode != AirPumpSetting.PRESSURE) {
            if (context.level.isClientSide) openSizeScreen(hub, tire, mode, context.hand)
        } else if (!context.level.isClientSide) {
            val decrease = player.isShiftKeyDown
            val pressure = AirPumpSetting.PRESSURE.value(tire)
            val next = if (decrease) (pressure - 1).coerceAtLeast(0) / 20 * 20 else (pressure / 20 + 1) * 20
            if (hub.setTireSetting(AirPumpSetting.PRESSURE, next, player))
                finishStroke(player, context.hand, hub, decrease)
        }
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    @Environment(EnvType.CLIENT)
    private fun openSizeScreen(hub: WheelHubBlockEntity, tire: WheelTire, mode: AirPumpSetting, hand: InteractionHand) =
        ScreenOpener.open(TireSizeScreen(hub, tire, mode, hand))

    internal fun finishStroke(player: Player, hand: InteractionHand, hub: WheelHubBlockEntity, decrease: Boolean) {
        val level = player.level()
        val stack = player.getItemInHand(hand)
        stack.orCreateTag.putLong("PumpStroke", level.gameTime)
        val sound = if (decrease) ClockworkSounds.TIRE_AIR_RELEASE else ClockworkSounds.TIRE_PUMP
        sound.playOnServer(level, hub.blockPos)
        player.cooldowns.addCooldown(this, STROKE_TICKS)
        if (!BacktankUtil.canAbsorbDamage(player, USES_PER_TANK))
            stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(hand) }
    }

    companion object {
        const val STROKE_TICKS = 9
        private const val USES_PER_TANK = 1000
    }
}
