package ca.mcmaster.se2aa4.island.teamXXX;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutOfRangeCheckerTest {

    private final OutOfRangeChecker checker = new OutOfRangeChecker();

    @Test
    void testNorthInBounds() {
        assertFalse(checker.wouldGoOutOfBounds("NORTH", 80, 150));
    }

    @Test
    void testNorthOutOfBounds() {
        assertTrue(checker.wouldGoOutOfBounds("NORTH", 80, 170));
    }

    @Test
    void testSouthInBounds() {
        assertFalse(checker.wouldGoOutOfBounds("SOUTH", 80, 5));
    }

    @Test
    void testSouthOutOfBounds() {
        assertTrue(checker.wouldGoOutOfBounds("SOUTH", 80, 1));
    }

    @Test
    void testEastInBounds() {
        assertFalse(checker.wouldGoOutOfBounds("EAST", 150, 80));
    }

    @Test
    void testEastOutOfBounds() {
        assertTrue(checker.wouldGoOutOfBounds("EAST", 170, 80));
    }

    @Test
    void testWestInBounds() {
        assertFalse(checker.wouldGoOutOfBounds("WEST", 5, 80));
    }

    @Test
    void testWestOutOfBounds() {
        assertTrue(checker.wouldGoOutOfBounds("WEST", 1, 80));
    }

    @Test
    void testInvalidDirectionNoMovement() {
        //if direction isn't recognized, then it should return false
        assertFalse(checker.wouldGoOutOfBounds("UP", 1, 1));
    }

    @Test
    void testCaseInsensitiveDirection() {
        assertTrue(checker.wouldGoOutOfBounds("nOrTh", 80, 160));
        assertTrue(checker.wouldGoOutOfBounds("SoUth", 80, 1));
        assertFalse(checker.wouldGoOutOfBounds("eAsT", 159, 80));
    }
}
