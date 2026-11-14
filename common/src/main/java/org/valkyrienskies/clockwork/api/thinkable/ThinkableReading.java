package org.valkyrienskies.clockwork.api.thinkable;

import net.minecraft.network.chat.Component;

/** Stable reading ID (1–128 characters) and its display name, including units. Combobulators list up to max 32 readings */
public record ThinkableReading(String id, Component name) {}
