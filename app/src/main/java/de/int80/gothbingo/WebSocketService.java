package de.int80.gothbingo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class WebSocketService extends Service {

    private final String TAG = WebSocketService.class.getName();

    public void setParentActivity(Context parentActivity) {
        this.parentActivity = parentActivity;
    }

    private Context parentActivity;

    public class LocalBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    private final LocalBinder mBinder = new LocalBinder();

    public WebSocketService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void moveToForeground() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.service_notification_text));
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);

        startForeground(1, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        moveToForeground();
        return START_STICKY;
    }

    public void stop() {
        stopForeground(true);
        stopSelf();
    }
}
