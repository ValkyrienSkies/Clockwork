package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.lwjgl.glfw.GLFW

class PlottingContextMenuTest {
    @Test fun menuStaysOnScreenAtBottomRightAndScrollsOnSmallScreens() {
        var selected = -1
        val actions = (0..9).map { index -> PlottingMenuAction(Component.literal("Action $index")) { selected = index } }
        val menu = PlottingContextMenu(319, 119, 140, 320, 120, actions) {}
        assertTrue(menu.x >= 4 && menu.y >= 4)
        assertTrue(menu.x + menu.width <= 316 && menu.y + menu.height <= 116)
        menu.scroll(-100.0)
        menu.mouseClicked(menu.x + 10.0, menu.y + menu.height - 10.0, 0)
        assertEquals(9, selected)
    }

    @Test fun disabledRowsDoNotRunOrDismissAndSelectionClosesBeforeAction() {
        val events = mutableListOf<String>()
        val menu = PlottingContextMenu(10, 10, 140, 320, 240, listOf(
            PlottingMenuAction(Component.literal("Disabled"), false) { events.add("disabled") },
            PlottingMenuAction(Component.literal("Delete"), destructive = true) { events.add("delete") }
        )) { events.add("close") }
        assertTrue(menu.mouseClicked(20.0, 20.0, 0))
        assertTrue(events.isEmpty())
        assertTrue(menu.mouseClicked(20.0, 38.0, 0))
        assertEquals(listOf("close", "delete"), events)
    }

    @Test fun outsideClickAndEscapeDismissWithoutInvokingActions() {
        var closed = 0
        var invoked = false
        val menu = PlottingContextMenu(10, 10, 140, 320, 240,
            listOf(PlottingMenuAction(Component.literal("Delete")) { invoked = true })) { closed++ }
        assertTrue(menu.mouseClicked(250.0, 150.0, 0))
        assertTrue(menu.keyPressed(GLFW.GLFW_KEY_ESCAPE))
        assertEquals(2, closed)
        assertFalse(invoked)
    }

    @Test fun keyboardSkipsDisabledActionsAndConsumesUnrelatedKeys() {
        var selected = -1
        val actions = (0..2).map { index -> PlottingMenuAction(Component.literal("Action $index"), index != 0) { selected = index } }
        val menu = PlottingContextMenu(10, 10, 140, 320, 240, actions) {}
        assertTrue(menu.keyPressed(GLFW.GLFW_KEY_DELETE))
        assertEquals(-1, selected)
        menu.keyPressed(GLFW.GLFW_KEY_DOWN)
        menu.keyPressed(GLFW.GLFW_KEY_ENTER)
        assertEquals(1, selected)
        menu.keyPressed(GLFW.GLFW_KEY_UP)
        menu.keyPressed(GLFW.GLFW_KEY_ENTER)
        assertEquals(2, selected)
    }
}
