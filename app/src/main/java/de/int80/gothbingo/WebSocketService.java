package de.int80.gothbingo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class WebSocketService extends Service {

    private static final String TAG = WebSocketService.class.getSimpleName();
    private static WebSocketService theInstance;
    public static final String PLAYER_NAME_KEY = WebSocketService.class.getName() + "PLAYER_NAME";
    public static final String GAME_ID_KEY = WebSocketService.class.getName() + "GAME_ID";

    class LocalBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    public void setParentActivity(MainActivity parentActivity) {
        this.parentActivity = parentActivity;
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

    private final LocalBinder mBinder = new LocalBinder();
    private MainActivity parentActivity;
    private String playerName;
    private String gameID;
    private WebSocket connection;
    private int currentGameNumber;
    private String lastWinner;
    private boolean hasWinner;
    private boolean localWin;
    private boolean terminated;
    private long lastConnect;

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

    private Notification makeNotification (String message, boolean background) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(message);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setPriority(background ? NotificationCompat.PRIORITY_MIN : NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(makeNotificationClickAction());

        Notification notification = builder.build();
        if (!background) {
            notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = getResources().getColor(R.color.colorPrimary);
            notification.ledOffMS = 2000;
            notification.ledOnMS = 1000;
        }

        return notification;
    }

    private void moveToForeground() {
        startForeground(1, makeNotification(getString(R.string.service_notification_text), true));
    }

    private void doConnect(OkHttpClient client, Request request) {
        connection = client.newWebSocket(request, new MessageHandler(this));
        lastConnect = System.currentTimeMillis();
    }

    private void scheduleDelayedConnect(final OkHttpClient client, final Request request) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                doConnect(client, request);
            }
        };

        try {
            new Timer().schedule(task, lastConnect + 30000 - System.currentTimeMillis());
        } catch (IllegalArgumentException e) {
            //already expired
            doConnect(client, request);
        }
    }

    public void connectToServer(boolean delayed) {
        Request.Builder requestBuilder = new Request.Builder();
        final Request request = requestBuilder.url("wss://int80.de/bingo/server").build();

        final OkHttpClient client = new OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).build();

        if (delayed)
            scheduleDelayedConnect(client, request);
        else
            doConnect(client, request);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        theInstance = this;

        moveToForeground();

        playerName = intent.getStringExtra(PLAYER_NAME_KEY);
        gameID = intent.getStringExtra(GAME_ID_KEY);

        if (connection == null)
            connectToServer(false);

        return START_STICKY;
    }

    public void stop() {
        connection.close(1001, null);

        stopForeground(true);
        stopSelf();
        terminated = true;
        theInstance = null;
    }

    private void playWinSound() {
        AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.win_sound);

        MediaPlayer player = new MediaPlayer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build();
            player.setAudioAttributes(attr);
        } else
            player.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });

        try {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        player.start();
    }

    private void handleGameEnd() {
        hasWinner = true;
        connection.close(1000, null);
    }

    public void startNewGame() {
        currentGameNumber = 0;
        hasWinner = false;
        connectToServer(false);
    }

    public void handleLoss(int gameNumber, String winner) {
        lastWinner = winner;
        currentGameNumber = gameNumber;
        localWin = false;

        final String winMessage = winner + " " + getString(R.string.lose_message);

        if (parentActivity != null) {
            parentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    parentActivity.displayWinMessage(winMessage);
                }
            });
        } else {
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(2, makeNotification(winMessage, false));
            } else {
                Log.e(TAG, "Failed to get notification manager");
            }
        }

        playWinSound();
        handleGameEnd();
    }

    public void handleWin() {
        connection.send("WIN;" + gameID + ";" + currentGameNumber + ";" + playerName);
        currentGameNumber++;
        localWin = true;
        playWinSound();
        handleGameEnd();
    }
}
