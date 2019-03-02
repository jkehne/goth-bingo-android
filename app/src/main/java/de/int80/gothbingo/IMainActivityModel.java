package de.int80.gothbingo;

import java.util.List;

public interface IMainActivityModel {
    void setPresenter(IMainActivityPresenter presenter);
    String getPlayerName();
    String getGameID();
    void showWinMessage(String message);
    void onActivityCreated(String playerName, String gameID);
    void handleFieldClicked(int x, int y);
    void startNewGame();
    void onGameExit();
    void onFieldContentFetcherComplete(List<String> fields, Throwable error);
}
