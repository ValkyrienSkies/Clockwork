package org.valkyrienskies.clockwork.forge

import net.minecraft.server.level.ServerLevel
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity

class PhysBearingForgeEvents {
    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level
        if (level !is ServerLevel) return

        val be = level.getBlockEntity(event.pos) as? PhysBearingBlockEntity ?: return
        if (!be.isRunning) return

        // Make breaking a running phys bearing behave like right-clicking to stop:
        // start alignment/disassembly, then automatically break the block once disassembled.
        event.isCanceled = true

        val dropItems = !event.player.abilities.instabuild
        be.requestBreakAfterDisassemble(dropItems)
        be.requestDisassembleFromBreak()
    }
}
