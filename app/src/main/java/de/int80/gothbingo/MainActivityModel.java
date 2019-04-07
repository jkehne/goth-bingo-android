package de.int80.gothbingo;

import android.os.AsyncTask;

import java.util.Collections;
import java.util.List;

public class MainActivityModel implements IMainActivityModel {

    private static final String TAG = MainActivityModel.class.getSimpleName();

    private IMainActivityPresenter presenter;
    private GameState gameState;
    private WebSocketServiceConnection backgroundServiceConnection;
    private FieldContentFetcher fieldContentFetcher;
    private static MainActivityModel theInstance;

    static MainActivityModel getInstance() {
        if (theInstance == null)
            theInstance = new MainActivityModel();

        return theInstance;
    }

    private MainActivityModel() {
        gameState = new GameState();
    }

    @Override
    public void setPresenter(IMainActivityPresenter presenter) {
        this.presenter = presenter;
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

        if (fieldContentFetcher != null) {
            presenter.showProgressDialog(GothBingo.getContext().getString(R.string.fields_downloading_message));
            return;
        }

        if (gameState.getAllFields() == null) {
            startNewGame();
            return;
        }

        presenter.resetBoard(gameState);
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
        WebSocketService service = backgroundServiceConnection.getService();
        if (service != null)
            service.startNewGame();

        resetGameState();

        fieldContentFetcher = new FieldContentFetcher(this);
        fieldContentFetcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (presenter != null)
            presenter.showProgressDialog(GothBingo.getContext().getString(R.string.fields_downloading_message));
    }

    private void resetGameState() {
        String playerName = gameState.getPlayerName();
        String gameID = gameState.getGameID();

        gameState = new GameState();

        gameState.setPlayerName(playerName);
        gameState.setGameID(gameID);
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
        else if (fields != null) {
            Collections.shuffle(fields);
            gameState.setAllFields(fields);
        }

        presenter.resetBoard(gameState);
    }

    @Override
    public void onNumPlayersChanged(int numPlayers) {
        this.presenter.onNumPlayersChanged(numPlayers);
    }

    @Override
    public GameState getState() {
        return gameState;
    }
}
