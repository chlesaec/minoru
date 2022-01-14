package board

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class BoardTest {

    @Test
    fun match() {
        val block1 = arrayOf(
            arrayOf(true, true, true, true),
            arrayOf(true, true, true, true),
            arrayOf(true, true, true, true)
        )
        val indexes = mapOf('1' to 0, '2' to 1)
        val piece1 = Piece(
            '1', 0, 2, 1,
            arrayOf(arrayOf(true), arrayOf(true))
        )
        val piece2 = Piece(
            '2', 1, 1, 2,
            arrayOf(arrayOf(true, true))
        )

        val pos1p1 = PositionPiece(Coord(0, 0), piece1)
        val pos1p1Bis = PositionPiece(Coord(0, 0), piece1)
        val pos2p1 = PositionPiece(Coord(0, 1), piece1)

        val pos1p2 = PositionPiece(Coord(2, 1), piece2)

        val b1 = Board(
            height = 3,
            width = 4,
            blocks = block1,
            indexes = indexes,
            pieces = arrayOf(pos1p1, pos1p2))

        Assertions.assertTrue(b1.match(b1))
        val b2 = Board(
            height = 3,
            width = 4,
            blocks = block1,
            indexes = indexes,
            pieces = arrayOf(pos1p1, null))
        Assertions.assertTrue(b1.match(b2))

        val b3 = Board(
            height = 3,
            width = 4,
            blocks = block1,
            indexes = indexes,
            pieces = arrayOf(pos1p1Bis, pos1p2))
        Assertions.assertTrue(b1.match(b3))

        val b4 = Board(
            height = 3,
            width = 4,
            blocks = block1,
            indexes = indexes,
            pieces = arrayOf(pos1p2, pos1p2))
        Assertions.assertFalse(b1.match(b4))
    }
}