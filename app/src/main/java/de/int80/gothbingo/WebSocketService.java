package de.int80.gothbingo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

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

    private final LocalBinder mBinder = new LocalBinder();
    private MediaPlayer winSound;
    private MainActivity parentActivity;
    private String playerName;
    private String gameID;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        moveToForeground();

        if (winSound == null)
            winSound = MediaPlayer.create(getApplicationContext(), R.raw.win_sound);

        playerName = intent.getStringExtra(PLAYER_NAME_KEY);
        gameID = intent.getStringExtra(GAME_ID_KEY);

        return START_STICKY;
    }

    public void stop() {
        winSound.release();

        stopForeground(true);
        stopSelf();
    }

    public void playWinSoud() {
        winSound.start();
    }
}
