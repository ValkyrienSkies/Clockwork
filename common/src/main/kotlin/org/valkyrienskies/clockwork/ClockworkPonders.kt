package org.valkyrienskies.clockwork

import com.simibubi.create.AllBlocks
import com.simibubi.create.AllItems
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import com.tterrag.registrate.util.entry.ItemProviderEntry
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.createmod.ponder.foundation.PonderSceneBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.ponders.KineticPonders.redstoneResistor
import org.valkyrienskies.clockwork.content.ponders.OtherPonders.solid_delivery
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.altMeter
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.createShip
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.flap
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.gyro
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.smart_flap
import java.time.Clock

object ClockworkPonders {

    fun init(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        val HELPER: PonderSceneRegistrationHelper<ItemProviderEntry<*>> = helper.withKeyFunction { it.id }
        HELPER.forComponents(ClockworkItems.WANDERWAND, ClockworkBlocks.PHYSICS_INFUSER)
            .addStoryBoard(
                "wanderwand", ::createShip
            )
        HELPER.forComponents(ClockworkBlocks.REDSTONE_RESISTOR)
            .addStoryBoard(
                "resistor", ::redstoneResistor
            )

        HELPER.forComponents(ClockworkBlocks.ALT_METER)
            .addStoryBoard(
                "alt_meter", ::altMeter
            )
        HELPER.forComponents(ClockworkBlocks.ANDESITE_FLAP_BEARING, ClockworkBlocks.SMART_FLAP_BEARING, ClockworkBlocks.FLAP)
            .addStoryBoard(
                "flap_bearing", ::flap
            )
            .addStoryBoard(
                "smart_flap_bearing", ::smart_flap
            )
        HELPER.forComponents(ClockworkBlocks.GYRO).addStoryBoard(
            "gyro", ::gyro
        )
        HELPER.forComponents(ClockworkBlocks.DELIVERY_CANNON, ClockworkBlocks.DELIVERY_CHUTE)
            .addStoryBoard(
                "solid_delivery", ::solid_delivery
            )
    }

    fun PonderSceneBuilder.ponderLang(index: Int): String {
        return Component.translatable("${ClockworkMod.MOD_ID}.ponder.${this.scene.id.path}.text_$index").string
    }
}
