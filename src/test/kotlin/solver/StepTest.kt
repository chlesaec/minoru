package solver

import board.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StepTest {

    @Test
    fun apply() {
        val piece1 = Piece('1', 0,1, 1, Array(1){ Array(1){true} })
        val pos1 = PositionPiece(Coord(0,0), piece1)
        val pieces = Array<PositionPiece?>(1) { pos1 }
        val indexes = HashMap<Char, Int>()
        indexes['1'] = 0
        val board = Board(height = 2,
            width = 2,
            blocks = Array(2){ Array(2){ true }},
            indexes = indexes,
            pieces = pieces)

        val step1 : Step = Step.create(board, Move(0, Direction.RIGHT))
        val step2 = Step.create(step1, Move(0, Direction.DOWN))

        val boardRes = board.copy { step2.positionPieces[it] }
        val posRes : PositionPiece? = boardRes.pieces[0]
        Assertions.assertNotNull(posRes)
        Assertions.assertEquals(Coord(1, 1), posRes?.position)
    }
}