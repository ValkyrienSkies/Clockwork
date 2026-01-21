package org.valkyrienskies.clockwork;

import net.minecraft.core.Direction;

/**
 * This rather sus class exists for the purpose of storing a public static variable (clicked face)
 * to be set in MixinLinkedControllerItem and accessed from MixinLinkedControllerClientHandler
 */
public class LinkedControllerClientHandlerMixinStorage {
    public static Direction face;
}
