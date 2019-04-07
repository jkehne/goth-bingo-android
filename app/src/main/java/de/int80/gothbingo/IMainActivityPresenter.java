package de.int80.gothbingo;

interface IMainActivityPresenter {
    void showWinMessage(String message);
    void onPlayAgainButtonClicked();
    void resetBoard(GameState state);
    void onFieldsDownloadFailed(Throwable error);
    void showProgressDialog(String message);
    void dismissProgressDialog();
    void onGameExit();
    void onNumPlayersChanged(int numPlayers);
    void onActivityCreated(String playerName, String gameID);
    void onActivityRestored();
}
