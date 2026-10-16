package dev.luniiya.mctinytweak.mcp

/** Tool ranking shared by direct agent mining, independent of client and registry initialization. */
object MiningTools {
    data class Candidate(val slot: Int, val speed: Float, val harvests: Boolean, val remainingUses: Int? = null)

    fun best(candidates: List<Candidate>, selected: Int, requiresTool: Boolean): Candidate? = candidates
        .filter { it.slot in 0..35 && it.speed.isFinite() && it.speed > 0 && (it.remainingUses == null || it.remainingUses > 0) }
        .filter { !requiresTool || it.harvests }
        .maxWithOrNull(compareBy<Candidate> { it.speed }
            .thenBy { it.remainingUses == null } // Avoid wearing tools on blocks they cannot mine faster.
            .thenBy { it.slot == selected }
            .thenBy { it.slot < 9 }
            .thenBy { it.remainingUses ?: Int.MAX_VALUE })
}
