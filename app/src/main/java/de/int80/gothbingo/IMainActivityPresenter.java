package de.int80.gothbingo;

import android.util.Pair;

import java.util.List;

interface IMainActivityPresenter {
    void showWinMessage(String message);
    void onPlayAgainButtonClicked();
    void resetBoard(List<String> fields);
    void setCheckedFields(List<Pair<Integer, Integer>> checkedFields);
    void onFieldsDownloadFailed(Throwable error);
    void showProgressDialog(String message);
    void dismissProgressDialog();
    void onGameExit();
    void onNumPlayersChanged(int numPlayers);
}
