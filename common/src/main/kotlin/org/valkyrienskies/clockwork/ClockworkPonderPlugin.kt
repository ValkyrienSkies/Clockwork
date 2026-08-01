package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.RegistryEntry
import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class ClockworkPonderPlugin : PonderPlugin {

    override fun getModId(): String {
        return ClockworkMod.MOD_ID
    }

    override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        ClockworkPonders.init(helper)
    }

    override fun registerTags(helper: PonderTagRegistrationHelper<ResourceLocation?>) {
        helper.registerTag(CLOCKWORK_TAG)
            .addToIndex()
            .item(ClockworkBlocks.PHYSICS_INFUSER.get(), true, false)
            .title(Component.translatable("vs_clockwork.ponder.tag.cw_ponders").string)
            .description(Component.translatable("vs_clockwork.ponder.tag.cw_ponders.description").string)
            .register();

        val HELPER = helper.withKeyFunction { obj: RegistryEntry<*> -> obj.id }

        // I haven't been able to find a better way to do this
        // that isn't hardcoding all the blocks we have ponders for
        HELPER.addToTag(CLOCKWORK_TAG)
            .add(ClockworkItems.WANDERWAND)
            .add(ClockworkBlocks.PHYSICS_INFUSER)
            .add(ClockworkBlocks.REDSTONE_RESISTOR)
            .add(ClockworkBlocks.ALT_METER)
            .add(ClockworkBlocks.ANDESITE_FLAP_BEARING)
            .add(ClockworkBlocks.SMART_FLAP_BEARING)
            .add(ClockworkBlocks.FLAP)
            .add(ClockworkBlocks.WING)
            .add(ClockworkBlocks.GYRO)
            .add(ClockworkBlocks.DELIVERY_CANNON)
            .add(ClockworkBlocks.DELIVERY_CHUTE)
            .add(ClockworkBlocks.DUCT)
    }

    companion object {
        val CLOCKWORK_TAG = ClockworkMod.asResource("cw_ponders")
    }
}
