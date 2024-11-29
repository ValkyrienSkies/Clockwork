package org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot

import com.mojang.datafixers.util.Pair
import com.simibubi.create.AllItems
import com.simibubi.create.CreateClient
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox
import com.simibubi.create.foundation.utility.AdventureUtil
import com.simibubi.create.foundation.utility.Lang
import com.simibubi.create.foundation.utility.RaycastHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets


object FrequencySlotGlobals {
    fun tick() {
        val mc = Minecraft.getInstance()
        val target = mc.hitResult
        if (mc.level == null || target == null || target !is BlockHitResult || mc.player == null || mc.player!!.getItemInHand(InteractionHand.MAIN_HAND).descriptionId=="item.create.wrench") return

        val result = target
        val world = mc.level
        val pos = result.blockPos


        val behaviour = BlockEntityBehaviour.get(world, pos, FrequencySlotBehaviour.TYPE)
            ?: return


        val label: Component = Lang.translateDirect("logistics.firstFrequency")


        val bb = AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25)
        val hit = behaviour.testHit(target.getLocation())


        val box = ValueBox(label, bb, pos).passive(!hit)
        val empty = behaviour.frequency
            .stack
            .isEmpty

        if (!empty) box.wideOutline()

        CreateClient.OUTLINER.showValueBox(Pair.of(true, pos), box.transform(behaviour.slot))
            .highlightFace(result.direction)

        if (!hit) return

        val tip: MutableList<MutableComponent> = ArrayList()
        tip.add(label.copy())
        tip.add(
            Lang.translateDirect(if (empty) "logistics.filter.click_to_set" else "logistics.filter.click_to_replace")
        )
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip)

    }


    fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {


        if (!level.isClientSide) return InteractionResult.SUCCESS
        val pos = hit.blockPos
        if (player.isShiftKeyDown || player.isSpectator) return InteractionResult.PASS
        val behaviour = BlockEntityBehaviour.get(level, pos, FrequencySlotBehaviour.TYPE)
            ?: return InteractionResult.PASS
        if (AdventureUtil.isAdventure(player)) return InteractionResult.PASS
        val heldItem = player.getItemInHand(hand)
        val ray = RaycastHelper.rayTraceRange(level, player, 10.0)
            ?: return InteractionResult.PASS
        if (AllItems.WRENCH.isIn(heldItem)) return InteractionResult.PASS

        if (behaviour.testHit(ray.location)) {
            ClockworkPackets.sendToServer(UpdateFrequencySlotPacket(heldItem, pos))
            level.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f)
            return InteractionResult.SUCCESS

        }

        return InteractionResult.PASS
    }
}
