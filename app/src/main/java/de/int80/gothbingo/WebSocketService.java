package de.int80.gothbingo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class WebSocketService extends Service {

    private static final String TAG = WebSocketService.class.getSimpleName();

    private static WebSocketService theInstance;
    public static final String PLAYER_NAME_KEY = WebSocketService.class.getName() + "PLAYER_NAME";
    public static final String GAME_ID_KEY = WebSocketService.class.getName() + "GAME_ID";
    private static final String BACKGROUND_NOTIFICATION_CHANNEL = "background_notification";

    private static boolean haveNotificationChannel;

    class LocalBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    public static WebSocketService getInstance() {
        return theInstance;
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

    public boolean isLocalWin() {
        return localWin;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public int getNumPlayers() { return numPlayers; }

    public void setNumPlayers(final int numPlayers) {
        Log.d(TAG, "Number of players changed to " + numPlayers);
        this.numPlayers = numPlayers;
        this.activityModel.onNumPlayersChanged(numPlayers);
    }

    private final LocalBinder mBinder = new LocalBinder();
    private String playerName;
    private String gameID;
    private WebSocket connection;
    private int currentGameNumber;
    private String lastWinner;
    private boolean hasWinner;
    private boolean localWin;
    private boolean terminated;
    private long lastConnect;
    private int numPlayers;
    private IMainActivityModel activityModel;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setActivityModel(IMainActivityModel activityModel) {
        this.activityModel = activityModel;
    }

    private void moveToForeground() {
        startForeground(1, makeNotification(getString(R.string.service_notification_text)));
    }

    private void doConnect() {
        Request request = new Request.Builder().url(getString(R.string.websocket_url)).build();
        OkHttpClient client = HTTPClientFactory.getWebsocketClient();

        connection = client.newWebSocket(request, new MessageHandler(this));
        lastConnect = System.currentTimeMillis();
    }

    private void scheduleDelayedConnect() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                doConnect();
            }
        };

        try {
            new Timer().schedule(task, lastConnect + 30000 - System.currentTimeMillis());
        } catch (IllegalArgumentException e) {
            //already expired
            doConnect();
        }
    }

    public void connectToServer(boolean delayed) {
        if (delayed)
            scheduleDelayedConnect();
        else
            doConnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        theInstance = this;

        makeNotificationChannels();

        moveToForeground();

        playerName = intent.getStringExtra(PLAYER_NAME_KEY);
        gameID = intent.getStringExtra(GAME_ID_KEY);

        if (connection == null)
            connectToServer(false);

        return START_STICKY;
    }

    private void makeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !haveNotificationChannel) {
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;

            NotificationChannel backgroundNotificationChannel = new NotificationChannel(
                    BACKGROUND_NOTIFICATION_CHANNEL,
                    getString(R.string.background_notification_channel_name),
                    NotificationManager.IMPORTANCE_MIN);

            manager.createNotificationChannel(backgroundNotificationChannel);

            haveNotificationChannel = true;
        }
    }

    private Notification makeNotification (String message) {
        Notification.Builder builder = new Notification.Builder(GothBingo.getContext());
        builder.setContentTitle(GothBingo.getContext().getString(R.string.app_name));
        builder.setContentText(message);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setPriority(Notification.PRIORITY_MIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(BACKGROUND_NOTIFICATION_CHANNEL);
        builder.setContentIntent(makeNotificationClickAction());

        return builder.build();
    }

    private PendingIntent makeNotificationClickAction() {
        Intent resultIntent = new Intent(GothBingo.getContext(), LoginActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        return PendingIntent.getActivity(GothBingo.getContext(), 0, resultIntent, 0);
    }

    public void stop() {
        connection.close(1001, null);

        stopForeground(true);
        stopSelf();
        terminated = true;
        theInstance = null;
    }

    private void handleGameEnd() {
        hasWinner = true;
        connection.close(1000, null);
    }

    public void startNewGame() {
        // end old connection if one exists
        if(connection != null)
            connection.close(1000,null);
        currentGameNumber = 0;
        hasWinner = false;
        connectToServer(false);
    }

    public void handleLoss(int gameNumber, String winner) {
        lastWinner = winner;
        currentGameNumber = gameNumber;
        localWin = false;

        final String winMessage = winner + " " + getString(R.string.lose_message);

        activityModel.showWinMessage(winMessage);

        handleGameEnd();
    }

    public void handleWin() {
        connection.send("WIN;" + gameID + ";" + currentGameNumber + ";" + playerName);
        currentGameNumber++;
        localWin = true;
        handleGameEnd();
    }
}
