package de.int80.gothbingo;

import java.io.Serializable;
import java.util.ArrayList;

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

    public void setAllFields(ArrayList<String> allFields) {
        this.allFields = allFields;
    }

    public ArrayList<String> getAllFields() {
        return allFields;
    }

    private ArrayList<String> allFields;

    public String getLastEtag() {
        return lastEtag;
    }

    public void setLastEtag(String lastEtag) {
        this.lastEtag = lastEtag;
    }

    private String lastEtag;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getGameID() {
        return gameID;
    }

    public void setGameID(String gameID) {
        this.gameID = gameID;
    }

    private String playerName;
    private String gameID;

    public void toggleField(int x, int y) {
        checkedFields[x][y] = checkedFields[x][y] ? false : true;
    }

    public boolean isFieldChecked(int x, int y) {
        return checkedFields[x][y];
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
