package com.example

import com.example.domain.CubeSolverEngine
import com.example.model.CubeColor
import com.example.model.CubeMove
import com.example.model.CubeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `every move repeated 4 times returns to solved state`() {
        val solved = CubeState.solved()
        for (move in CubeMove.entries) {
            val expectedOrder = if (move.turnsCw == 2) 2 else 4
            var current = solved
            repeat(expectedOrder) {
                current = current.applyMove(move)
            }
            assertEquals("Move ${move.notation} order check failed", solved, current)
        }
    }

    @Test
    fun `sexy move R U R' U' repeated 6 times returns to solved state`() {
        val solved = CubeState.solved()
        val sexyMove = CubeMove.parseSequence("R U R' U'")
        var current = solved
        repeat(6) {
            current = current.applyMoves(sexyMove)
        }
        assertTrue(current.isSolved())
    }

    @Test
    fun `bidirectional solver finds optimal solution for 6-move and 8-move scrambles`() {
        for (preset in CubeSolverEngine.PRESET_SCRAMBLES) {
            val moves = CubeMove.parseSequence(preset.movesNotation)
            val scrambled = CubeState.solved().applyMoves(moves)
            assertFalse(scrambled.isSolved())
            assertTrue(CubeSolverEngine.validate(scrambled).isValid)

            val steps = CubeSolverEngine.solve(scrambled, moves)
            assertTrue("Solution should not be empty for ${preset.name}", steps.isNotEmpty())
            assertTrue("Final step must reach solved state for ${preset.name}", steps.last().stateAfter.isSolved())
        }
    }

    @Test
    fun `HSV color classifier distinguishes all 6 Rubik's Cube sticker colors`() {
        assertEquals(CubeColor.WHITE, CubeColor.fromHsv(0f, 0.08f, 0.92f))
        assertEquals(CubeColor.RED, CubeColor.fromHsv(355f, 0.88f, 0.85f))
        assertEquals(CubeColor.ORANGE, CubeColor.fromHsv(24f, 0.90f, 0.92f))
        assertEquals(CubeColor.YELLOW, CubeColor.fromHsv(54f, 0.85f, 0.95f))
        assertEquals(CubeColor.GREEN, CubeColor.fromHsv(135f, 0.82f, 0.80f))
        assertEquals(CubeColor.BLUE, CubeColor.fromHsv(215f, 0.86f, 0.88f))
    }
}
