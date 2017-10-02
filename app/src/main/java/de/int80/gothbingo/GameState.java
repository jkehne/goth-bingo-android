package de.int80.gothbingo;

import java.io.Serializable;

/**
 * Created by jens on 29.09.17.
 */

public class GameState implements Serializable {
    private boolean checkedFields[][] = {
            {false, false, false, false, false},
            {false, false, false, false, false},
            {false, false, true,  false, false},
            {false, false, false, false, false},
            {false, false, false, false, false}
    };

    public void toggleField(int x, int y) {
        checkedFields[x][y] = checkedFields[x][y] ? false : true;
    }

    public boolean hasFullRow() {
        int i, j;
        boolean row;
        boolean column;
        boolean diagonalDown = true;
        boolean diagonalUp = true;

        for (i = 0; i < 5; ++i) {
            row = true;
            column = true;
            for (j = 0; j < 5; ++j) {
                column &= checkedFields[i][j];
                row &= checkedFields[j][i];
            }
            if (row || column)
                return true;

            diagonalDown &= checkedFields[i][i];
            diagonalUp &= checkedFields[i][4-i];
        }
        return (diagonalDown || diagonalUp);
    }
}
