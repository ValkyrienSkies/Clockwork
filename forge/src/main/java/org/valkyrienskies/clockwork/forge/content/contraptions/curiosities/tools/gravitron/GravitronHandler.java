package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.valkyrienskies.clockwork.ClockworkItems;
import org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool.ToolType;

import java.util.Vector;

public class GravitronHandler implements IGuiOverlay {

    GravitronSelectionScreen selectionScreen;
    private boolean active;
    private ToolType currentTool;
    int activeHotbarSlot = 0;
    ItemStack activeSchematicItem = null;
    SchematicHotbarSlotOverlay overlay;

    public GravitronHandler() {
        overlay = new SchematicHotbarSlotOverlay();
        currentTool = ToolType.GRAB;
        selectionScreen = new GravitronSelectionScreen(ToolType.getTools(), this::equip);
    }



    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        render(graphics, partialTicks, width, height);

    }

    public void tick() {
        var mc = Minecraft.getInstance();
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            if (active) {
                active = false;
                activeHotbarSlot = 0;
                activeSchematicItem = null;
            }
            return;
        }
        var player = mc.player;
        var stack = findBlueprintInHand(player);
        if (stack == null) {
            active = false;
            if (activeSchematicItem != null && itemLost(player)) {
                activeHotbarSlot = 0;
                activeSchematicItem = null;
            }
            return;
        }
        init(player);
        if (!active) return;

        selectionScreen.update();
    }

    public void render(GuiGraphics graphics, float partialTicks,int width, int height) {
        if (Minecraft.getInstance().options.hideGui || !active) {
            return;
        }
        if (activeSchematicItem != null) {
            overlay.renderOn(graphics, activeHotbarSlot);
        }

        currentTool.tool.renderOverlay(graphics, partialTicks, width, height);
        selectionScreen.renderPassive(graphics, partialTicks);
    }

    private void init(LocalPlayer player) {
        active = true;
    }

    private boolean itemLost(Player player) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (!ItemStack.matches(player.getInventory()
                    .getItem(i), activeSchematicItem))
                continue;
            return false;
        }
        return true;
    }

    public void equip(ToolType tool) {
        this.currentTool = tool;
        currentTool.tool.init();
    }

    private ItemStack findBlueprintInHand(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!ClockworkItems.GRAVITRON.isIn(stack))
            return null;
        if (!stack.hasTag())
            return null;

        activeSchematicItem = stack;
        activeHotbarSlot = player.getInventory().selected;
        return stack;
    }

    public boolean onMouseInput(int button, boolean pressed) {
        if (!active)
            return false;
        if (!pressed || button != 1)
            return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.isShiftKeyDown())
            return false;
        return currentTool.tool.handleRightClick();
    }

    public void onKeyInput(int key, boolean pressed) {
        if (!active)
            return;
        if (key != AllKeys.TOOL_MENU.getBoundCode())
            return;

        if (pressed && !selectionScreen.focused)
            selectionScreen.focused = true;
        if (!pressed && selectionScreen.focused) {
            selectionScreen.focused = false;
            selectionScreen.onClose();
        }
    }

    public boolean mouseScrolled(double delta) {
        if (!active)
            return false;

        if (selectionScreen.focused) {
            selectionScreen.cycle((int) delta);
            return true;
        }
        if (AllKeys.ctrlDown())
            return currentTool.tool
                    .handleMouseWheel(delta);
        return false;
    }

    public static void init() {

    }
}