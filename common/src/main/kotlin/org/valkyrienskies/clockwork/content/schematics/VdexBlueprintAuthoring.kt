package org.valkyrienskies.clockwork.content.schematics

import org.valkyrienskies.mod.common.schematic.VdexData

data class VdexBlueprintAuthoring(val name: String, val author: String, val description: String) {
    fun apply(data: VdexData): VdexData {
        require(name.isNotBlank() && name.length <= NAME_LIMIT) { "Enter a schematic name" }
        require(author.isNotBlank() && author.length <= AUTHOR_LIMIT) { "Enter an author" }
        require(description.length <= DESCRIPTION_LIMIT) { "Description is too long" }
        require((name + author).none { it.isISOControl() || it == '\u00a7' } &&
            description.none { (it.isISOControl() && it != '\n') || it == '\u00a7' }) { "Invalid schematic text" }
        return data.copy(metadata = data.metadata.copy(social = data.metadata.social.copy(
            name = name.trim(), author = author.trim(), description = description.trim())))
    }

    companion object {
        const val NAME_LIMIT = 80
        const val AUTHOR_LIMIT = 80
        const val DESCRIPTION_LIMIT = 2048
    }
}
