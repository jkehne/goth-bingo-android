package de.int80.gothbingo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by jens on 27.10.17.
 */

public class WebSocketServiceConnection implements ServiceConnection {
    public WebSocketService getService() {
        return mService;
    }

    private WebSocketService mService;
    private Context mContext;

    public WebSocketServiceConnection(Context context) {
        mContext = context;
    }

    public boolean startService() {
        Intent intent = new Intent(mContext, WebSocketService.class);
        mContext.startService(intent);
        return mContext.bindService(intent, this, 0);
    }

    public void stopService() {
        mContext.unbindService(this);
        mService.stop();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        WebSocketService.LocalBinder binder = (WebSocketService.LocalBinder)iBinder;
        mService = binder.getService();
        mService.setParentActivity(mContext);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mService.setParentActivity(null);
    }
}
