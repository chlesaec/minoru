package solver

import board.Coord
import board.Direction
import board.Move
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StepContainerTest {

    @Test
    fun add() {
        val container = StepContainer()
        val c1 = Coord(0, 2)
        val c1Bis = Coord(0, 2)
        val c2 = Coord(1, 3)
        val c3 = Coord(2, 0)

        val a1 : Array<Coord?> = arrayOf(c1, c2)
        val a2 : Array<Coord?> = arrayOf(c1, c3)
        val a1Bis : Array<Coord?> = arrayOf(c1, c2)
        val a1Bis2 : Array<Coord?> = arrayOf(c1Bis, c2)

        val m = Move(0, Direction.RIGHT)
        val step1 = Step(a1, m)
        val step2 = Step(a2, m)

        val step1Bis = Step(a1Bis, m)
        val step1Bis2 = Step(a1Bis2, m)

        Assertions.assertTrue(container.add(step1), "step 1 not insert")
        Assertions.assertFalse(container.add(step1), "step 1 double insert")

        Assertions.assertTrue(container.add(step2), "step 2 not insert")
        Assertions.assertFalse(container.add(step1Bis), "step 1 bis double insert")
        Assertions.assertFalse(container.add(step1Bis2), "step 1 bis2 double insert")
    }
}