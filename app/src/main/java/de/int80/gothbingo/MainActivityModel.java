package de.int80.gothbingo;

import android.os.AsyncTask;

import java.util.List;

public class MainActivityModel implements IMainActivityModel {

    private static final String TAG = MainActivityModel.class.getSimpleName();
    private IMainActivityPresenter presenter;
    private final GameState gameState;
    private WebSocketServiceConnection backgroundServiceConnection;
    private FieldContentFetcher fieldContentFetcher;

    MainActivityModel() {
        gameState = new GameState();
        startNewGame();
    }

    @Override
    public void setPresenter(IMainActivityPresenter presenter) {
        this.presenter = presenter;
        if (fieldContentFetcher != null)
            presenter.showProgressDialog(GothBingo.getContext().getString(R.string.fields_downloading_message));
        else {
            presenter.resetBoard(gameState.getAllFields());
            presenter.setCheckedFields(gameState.getCheckedFieldsList());
        }
    }

    @Override
    public String getPlayerName() {
        return gameState.getPlayerName();
    }

    @Override
    public String getGameID() {
        return gameState.getGameID();
    }

    @Override
    public void showWinMessage(String message) {
        presenter.showWinMessage(message);
    }

    @Override
    public void onActivityCreated(String playerName, String gameID) {
        if (playerName != null)
            gameState.setPlayerName(playerName);
        if (gameID != null)
            gameState.setGameID(gameID);

        backgroundServiceConnection = new WebSocketServiceConnection(this);
    }

    @Override
    public void handleFieldClicked(int x, int y) {
        gameState.toggleField(x, y);

        if (gameState.hasFullRow()) {
            showWinMessage(GothBingo.getContext().getString(R.string.win_message));
            backgroundServiceConnection.getService().handleWin();
        }
    }

    @Override
    public void startNewGame() {
        backgroundServiceConnection.getService().startNewGame();

        fieldContentFetcher = new FieldContentFetcher(this);
        fieldContentFetcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (presenter != null)
            presenter.showProgressDialog(GothBingo.getContext().getString(R.string.fields_downloading_message));
    }

    @Override
    public void onGameExit() {
        backgroundServiceConnection.getService().stop();
    }

    @Override
    public void onFieldContentFetcherComplete(List<String> fields, Throwable error) {
        fieldContentFetcher = null;
        presenter.dismissProgressDialog();

        if (error != null) {
            presenter.onFieldsDownloadFailed(error);
            return;
        }
        else if (fields != null)
            gameState.setAllFields(fields);

        presenter.resetBoard(gameState.getAllFields());
    }

    @Override
    public void onNumPlayersChanged(int numPlayers) {
        this.presenter.onNumPlayersChanged(numPlayers);
    }

}
