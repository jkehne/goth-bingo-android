package de.int80.gothbingo;

import android.support.annotation.Nullable;
import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

class MessageHandler extends WebSocketListener {
    private static final String TAG = MessageHandler.class.getSimpleName();
    private final WebSocketService parentService;

    MessageHandler(WebSocketService context) {
        parentService = context;
    }

    private void handleUnknownMessage(String message) {
        Log.e(TAG, "Unknown message received: " + message);
    }

    private void handleSignin(String params) {
        String[] tokens = params.split(";");
        int gameNumber = Integer.valueOf(tokens[0]);
        String winner;
        try {
            winner = tokens[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            winner = "";
        }

        if (parentService.getCurrentGameNumber() == 0) {
            parentService.setCurrentGameNumber(gameNumber);
            return;
        }

        if (parentService.getCurrentGameNumber() != gameNumber) {
            parentService.handleLoss(gameNumber, winner);
            return;
        }

        parentService.setCurrentGameNumber(gameNumber);
    }

    private void handleWin(String params) {
        String[] tokens = params.split(";");
        int gameNumber = Integer.valueOf(tokens[0]);
        String winner = tokens[1];

        parentService.handleLoss(gameNumber, winner);
    }

    private void handleNumPlayersChange(String params) {
        String[] tokens = params.split(";");
        parentService.setNumPlayers(Integer.valueOf(tokens[0]));
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if (!text.contains(";")) {
            handleUnknownMessage(text);
            return;
        }

        String[] tokens = text.split(";", 2);

        switch (tokens[0]) {
            case "SIGNIN":
                handleSignin(tokens[1]);
                break;
            case "WIN":
                handleWin(tokens[1]);
                break;
            case "PLAYERS":
                handleNumPlayersChange(tokens[1]);
                break;
            default:
                handleUnknownMessage(text);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send("SIGNIN;" + parentService.getGameID());
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        if (!parentService.hasWinner() && !parentService.isTerminated()) {
            Log.e(TAG, "Lost connection to server: " + ((response != null) ? response.message() : ""), t);
            parentService.connectToServer(true);
        }
    }
}
