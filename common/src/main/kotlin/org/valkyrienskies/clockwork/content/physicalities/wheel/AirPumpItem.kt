package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.simibubi.create.content.equipment.armor.BacktankUtil
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

enum class AirPumpSetting(val id: String, val step: Int) {
    PRESSURE("pressure", 5), WIDTH("width", 1), THICKNESS("thickness", 1);

    val label get() = Component.translatable("block.vs_clockwork.wheel_hub.$id")

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

    private fun setting(stack: ItemStack) =
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
        if (!context.level.isClientSide) {
            val stack = context.itemInHand
            val mode = setting(stack)
            val decrease = player.isShiftKeyDown
            val next = mode.value(tire) + (if (decrease) -mode.step else mode.step)
            if (hub.setTireSetting(mode, next, player)) {
                stack.orCreateTag.putLong("PumpStroke", context.level.gameTime)
                val sound = if (decrease) ClockworkSounds.TIRE_AIR_RELEASE else ClockworkSounds.TIRE_PUMP
                sound.playOnServer(context.level, hub.blockPos)
                player.cooldowns.addCooldown(this, STROKE_TICKS)
                if (!BacktankUtil.canAbsorbDamage(player, USES_PER_TANK)) {
                    stack.hurtAndBreak(1, player) { it.broadcastBreakEvent(context.hand) }
                }
            }
        }
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    companion object {
        const val STROKE_TICKS = 9
        private const val USES_PER_TANK = 1000
    }
}
