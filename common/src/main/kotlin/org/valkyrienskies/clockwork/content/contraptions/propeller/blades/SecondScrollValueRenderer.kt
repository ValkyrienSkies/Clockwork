package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.AllBlocks
import com.simibubi.create.CreateClient
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer
import com.simibubi.create.foundation.utility.AdventureUtil
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.outliner.Outliner
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object SecondScrollValueRenderer: ScrollValueRenderer() {
    fun tickSecond() {
        val mc = Minecraft.getInstance()
        if (mc.level == null) return
        val target = mc.hitResult
        if (target == null || target !is BlockHitResult) return

        val result = target
        val world = mc.level
        val pos = result.blockPos
        val face = result.direction

        val behaviour = BlockEntityBehaviour.get(world, pos, LengthScrollValueBehaviour.LENGTH_TYPE)
            ?: return
        if (!behaviour.isActive) {
            Outliner.getInstance().remove(Vec3.atCenterOf(pos))
            return
        }
        val mainhandItem = mc.player!!.getItemInHand(InteractionHand.MAIN_HAND)
        val clipboard = AllBlocks.CLIPBOARD.isIn(mainhandItem)
        val highlight = behaviour.testHit(target.getLocation()) && !clipboard

        addSecondBox(world, pos, face, behaviour, highlight)

        if (!highlight) return

        val tip: MutableList<MutableComponent> = ArrayList()
        tip.add(behaviour.label.copy())
        tip.add(CreateLang.translateDirect("gui.value_settings.hold_to_edit"))
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip)
    }

    private fun addSecondBox(
        world: ClientLevel?, pos: BlockPos, face: Direction, behaviour: ScrollValueBehaviour,
        highlight: Boolean
    ) {
        val bb = AABB(Vec3.ZERO, Vec3.ZERO).inflate(.5)
            .contract(0.0, 0.0, -.5)
            .move(0.0, 0.0, -.125)
        val label = behaviour.label

        val box = ValueBox.TextValueBox(label, bb, pos, Component.literal(behaviour.formatValue()))


        if (!AdventureUtil.isAdventure(Minecraft.getInstance().player)) box.passive(!highlight)
            .wideOutline()

        Outliner.getInstance().showOutline(Vec3.atCenterOf(pos), box.transform(behaviour.slotPositioning))
            .highlightFace(face)
    }

    fun init() {}
}
