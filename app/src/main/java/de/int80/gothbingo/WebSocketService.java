package de.int80.gothbingo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class WebSocketService extends Service {

    public static final String PLAYER_NAME_KEY = WebSocketService.class.getName() + "PLAYER_NAME";
    public static final String GAME_ID_KEY = WebSocketService.class.getName() + "GAME_ID";

    public class LocalBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    public void setParentActivity(MainActivity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public String getGameID() {
        return gameID;
    }

    public void setCurrentGameNumber(int currentGameNumber) {
        this.currentGameNumber = currentGameNumber;
    }

    public int getCurrentGameNumber() {
        return currentGameNumber;
    }

    public String getLastWinner() {
        return lastWinner;
    }

    public boolean hasWinner() {
        return hasWinner;
    }

    private final LocalBinder mBinder = new LocalBinder();
    private MediaPlayer winSound;
    private MainActivity parentActivity;
    private String playerName;
    private String gameID;
    private WebSocket connection;
    private int currentGameNumber;
    private String lastWinner;
    private boolean hasWinner;

    public WebSocketService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private PendingIntent makeNotificationClickAction() {
        Intent resultIntent = new Intent(this, LoginActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        return PendingIntent.getActivity(this, 0, resultIntent, 0);
    }

    private void moveToForeground() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.service_notification_text));
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setContentIntent(makeNotificationClickAction());

        startForeground(1, builder.build());
    }

    private WebSocket connectToServer() {
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.url("wss://int80.de/bingo/server").build();

        OkHttpClient client = new OkHttpClient();
        return client.newWebSocket(request, new MessageHandler(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        moveToForeground();

        if (winSound == null)
            winSound = MediaPlayer.create(getApplicationContext(), R.raw.win_sound);

        playerName = intent.getStringExtra(PLAYER_NAME_KEY);
        gameID = intent.getStringExtra(GAME_ID_KEY);

        if (connection == null)
            connection = connectToServer();

        return START_STICKY;
    }

    public void stop() {
        winSound.release();

        connection.close(1001, null);

        stopForeground(true);
        stopSelf();
    }

    private void playWinSoud() {
        winSound.start();
    }

    private void handleGameEnd() {
        hasWinner = true;
        connection.close(1000, null);
    }

    public void startNewGame() {
        hasWinner = false;
        connection = connectToServer();
    }

    public void handleLoss(int gameNumber, final String winner) {
        lastWinner = winner;
        currentGameNumber = gameNumber;

        if (parentActivity != null) {
            parentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    parentActivity.displayWinMessage(winner + " " + getString(R.string.lose_message));
                }
            });
        }

        playWinSoud();
        handleGameEnd();
    }

    public void handleWin() {
        connection.send("WIN;" + gameID + ";" + currentGameNumber + ";" + playerName);
        currentGameNumber++;
        playWinSoud();
        handleGameEnd();
    }
}
