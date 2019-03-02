package de.int80.gothbingo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.List;

public class BackgroundActivityPresenter implements IMainActivityPresenter {
    private static final String TAG = BackgroundActivityPresenter.class.getSimpleName();
    private static final String FOREGROUND_NOTIFICATION_CHANNEL = "win_message";

    private static boolean haveNotificationChannel = false;

    BackgroundActivityPresenter() {
        makeNotificationChannels();
    }

    private void playWinSound() {
        AssetFileDescriptor afd = GothBingo.getContext().getResources().openRawResourceFd(R.raw.win_sound);

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

    private Notification makeNotification (String message) {
        Notification.Builder builder = new Notification.Builder(GothBingo.getContext());
        builder.setContentTitle(GothBingo.getContext().getString(R.string.app_name));
        builder.setContentText(message);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(FOREGROUND_NOTIFICATION_CHANNEL);
        builder.setContentIntent(makeNotificationClickAction());
        builder.setSound(notificationSound());

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = ContextCompat.getColor(GothBingo.getContext(), R.color.colorPrimary);
        notification.ledOffMS = 2000;
        notification.ledOnMS = 1000;

        return notification;
    }

    private void makeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !haveNotificationChannel) {
            NotificationManager manager = (NotificationManager)GothBingo.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;

            NotificationChannel foregroundNotificationChannel = new NotificationChannel(
                    FOREGROUND_NOTIFICATION_CHANNEL,
                    GothBingo.getContext().getString(R.string.foreground_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            foregroundNotificationChannel.enableLights(true);
            foregroundNotificationChannel.setLightColor(ContextCompat.getColor(GothBingo.getContext(), R.color.colorPrimary));
            foregroundNotificationChannel.setSound(notificationSound(), null);

            manager.createNotificationChannel(foregroundNotificationChannel);

            haveNotificationChannel = true;
        }
    }

    private PendingIntent makeNotificationClickAction() {
        Intent resultIntent = new Intent(GothBingo.getContext(), LoginActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        return PendingIntent.getActivity(GothBingo.getContext(), 0, resultIntent, 0);
    }

    private Uri notificationSound() {
        Resources res = GothBingo.getContext().getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + res.getResourcePackageName(R.raw.win_sound)
                + '/' + res.getResourceTypeName(R.raw.win_sound)
                + '/' + res.getResourceEntryName(R.raw.win_sound));
    }

    @Override
    public void showWinMessage(String message) {
        NotificationManager manager = (NotificationManager)GothBingo.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
                manager.notify(2, makeNotification(message));
                playWinSound();
            } else {
                Log.e(TAG, "Failed to get notification manager");
            }
    }

    @Override
    public void onPlayAgainButtonClicked() {
        Log.e(TAG, "Play again button clicked while activity in background");
    }

    @Override
    public void resetBoard(List<String> fields) {
        // Can legitimately happen, but nothing to do here
    }

    @Override
    public void setCheckedFields(List<Pair<Integer, Integer>> checkedFields) {
        // Can legitimately happen, but nothing to do here
    }

    @Override
    public void onFieldsDownloadFailed(Throwable error) {
        // Can legitimately happen, but nothing to do here
    }

    @Override
    public void showProgressDialog(String message) {
        Log.e(TAG, "Download progress dialog requested while activity in background");
    }

    @Override
    public void dismissProgressDialog() {
        // Can legitimately happen, but nothing to do here
    }

    @Override
    public void onGameExit() {
        Log.e(TAG, "Game exit requested while activity in background");
    }

}
