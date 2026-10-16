package dev.luniiya.mctinytweak.hotbar

/**
 * Positions for a hotbar split into two groups: the first [GROUP_SIZE] slots (X C V B in the personal binds)
 * sit apart from the rest with a [GAP] between them. Built from the vanilla 182x22 sprite, where a 1px border
 * surrounds nine 20px slots.
 */
object HotbarLayout {
    const val SLOTS = 9
    const val GROUP_SIZE = 4
    const val GAP = 4
    const val SLOT_WIDTH = 20
    const val SPRITE_WIDTH = 182
    const val HEIGHT = 22

    /** Sprite columns copied for the first group: the left border and its slots. */
    const val FIRST_GROUP_U_END = 1 + GROUP_SIZE * SLOT_WIDTH
    /** Each group gets a closing border, so the bar is two pixels wider than vanilla plus the gap. */
    const val FIRST_PIECE_WIDTH = FIRST_GROUP_U_END + 1
    const val SECOND_PIECE_WIDTH = 1 + (SLOTS - GROUP_SIZE) * SLOT_WIDTH + 1
    const val WIDTH = FIRST_PIECE_WIDTH + GAP + SECOND_PIECE_WIDTH

    fun left(centerX: Int) = centerX - WIDTH / 2
    fun right(centerX: Int) = left(centerX) + WIDTH
    fun secondPieceX(centerX: Int) = left(centerX) + FIRST_PIECE_WIDTH + GAP

    fun isFirstGroup(slot: Int) = slot < GROUP_SIZE

    /** The x of a slot's 20px frame, inside the border. */
    fun frameX(slot: Int, centerX: Int): Int {
        require(slot in 0 until SLOTS) { "Hotbar slot must be between 0 and 8" }
        return if (isFirstGroup(slot)) left(centerX) + 1 + slot * SLOT_WIDTH
        else secondPieceX(centerX) + 1 + (slot - GROUP_SIZE) * SLOT_WIDTH
    }

    /** Vanilla draws items 2px into the frame and the selection box 2px outside it. */
    fun itemX(slot: Int, centerX: Int) = frameX(slot, centerX) + 2
    fun selectionX(slot: Int, centerX: Int) = frameX(slot, centerX) - 2
}
