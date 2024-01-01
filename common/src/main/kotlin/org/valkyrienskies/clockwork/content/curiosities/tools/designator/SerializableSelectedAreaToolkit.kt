package org.valkyrienskies.clockwork.content.curiosities.tools.designator

import net.minecraft.world.phys.AABB
import org.joml.primitives.AABBd

data class SerializableSelectedAreaToolkit(val selectedAreas: HashSet<AABBd>, val selectionClusters: HashSet<Set<AABBd>>)
