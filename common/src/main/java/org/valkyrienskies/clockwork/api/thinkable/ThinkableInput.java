package org.valkyrienskies.clockwork.api.thinkable;

import org.jetbrains.annotations.Nullable;

public record ThinkableInput(boolean connected, @Nullable Double value) {}
