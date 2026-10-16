package dev.luniiya.mctinytweak.mcp

data class WorldCreationSpec(val id: String, val name: String, val mode: String, val difficulty: String, val flat: Boolean, val commands: Boolean, val seed: String?) {
    init {
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}"))) { "id must be 1-64 letters, digits, underscores or hyphens, starting with a letter or digit" }
        require(name.isNotBlank() && name.length <= 128 && name.none { it.isISOControl() }) { "name must be nonblank text of at most 128 characters" }
        require(mode in setOf("survival", "creative")) { "mode must be survival or creative" }
        require(difficulty in setOf("peaceful", "easy", "normal", "hard")) { "difficulty must be peaceful, easy, normal or hard" }
        require(seed == null || seed.length <= 128) { "seed must be at most 128 characters" }
    }
}
