package de.int80.gothbingo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameStateTest {

    private GameState state;

    @Before
    public void setUp() {
        state = new GameState();
    }

    @After
    public void tearDown() {
        state = null;
    }

    @Test
    public void toggleField() {
        assertFalse(state.isFieldChecked(0, 0));
        state.toggleField(0,0);
        assertTrue(state.isFieldChecked(0,0));
        state.toggleField(0,0);
        assertFalse(state.isFieldChecked(0,0));
    }

    @Test
    public void hasFullRowHorizontalEdge() {
        assertFalse(state.hasFullRow());
        for (int i = 0; i < 5; i++) {
            state.toggleField(0, i);
        }
        assertTrue(state.hasFullRow());
        state.toggleField(0,0);
        assertFalse(state.hasFullRow());
    }

    @Test
    public void hasFullRowHorizontalCenter() {
        assertFalse(state.hasFullRow());
        for (int i = 0; i < 5; i++) {
            if (i == 2)
                continue;

            state.toggleField(2, i);
        }
        assertTrue(state.hasFullRow());
        state.toggleField(2,0);
        assertFalse(state.hasFullRow());
    }

    @Test
    public void hasFullRowVerticalEdge() {
        assertFalse(state.hasFullRow());
        for (int i = 0; i < 5; i++) {
            state.toggleField(i, 0);
        }
        assertTrue(state.hasFullRow());
        state.toggleField(0,0);
        assertFalse(state.hasFullRow());
    }

    @Test
    public void hasFullRowVerticalCenter() {
        assertFalse(state.hasFullRow());
        for (int i = 0; i < 5; i++) {
            if (i == 2)
                continue;

            state.toggleField(i, 2);
        }
        assertTrue(state.hasFullRow());
        state.toggleField(0,2);
        assertFalse(state.hasFullRow());
    }

    @Test
    public void hasFullRowDiagonalUp() {
        assertFalse(state.hasFullRow());
        for (int i = 0; i < 5; i++) {
            if (i == 2)
                continue;

            state.toggleField(i, i);
        }
        assertTrue(state.hasFullRow());
        state.toggleField(0,0);
        assertFalse(state.hasFullRow());
    }

    @Test
    public void hasFullRowDiagonalDown() {
        assertFalse(state.hasFullRow());
        for (int i = 0; i < 5; i++) {
            if (i == 2)
                continue;

            state.toggleField(i, 4-i);
        }
        assertTrue(state.hasFullRow());
        state.toggleField(0,4);
        assertFalse(state.hasFullRow());
    }
}