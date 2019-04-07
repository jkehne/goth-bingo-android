package de.int80.gothbingo;

import java.util.List;

public interface IMainActivity {
    void showProgressDialog(String message);
    void dismissProgressDialog();
    void showWinMessage(String message);
    void hideWinMessage();
    String getString(int resID);
    void toggleField(BingoFieldView field);
    void toggleField(int row, int col);
    void resetClickedState();
    void setFieldContents(List<String> fields);
    void showDownloadFailureDialog(Throwable t);
    void setNumPlayers(int numPlayers);
}
