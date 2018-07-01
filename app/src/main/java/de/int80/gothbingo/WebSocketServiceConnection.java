package de.int80.gothbingo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by jens on 27.10.17.
 */

class WebSocketServiceConnection implements ServiceConnection {

    public WebSocketService getService() {
        if (WebSocketService.getInstance() != null)
            return WebSocketService.getInstance();
        else
            return mService;
    }

    private WebSocketService mService;
    private final MainActivity mContext;

    WebSocketServiceConnection(MainActivity context) {
        mContext = context;

        GameState state = mContext.getState();

        if (WebSocketService.getInstance() != null) {
            MainActivity currentMain = MainActivity.getCurrentInstance();
            if (currentMain != null)
                currentMain.setNumPlayers(WebSocketService.getInstance().getNumPlayers());
            return;
        }

        Intent intent = new Intent(mContext, WebSocketService.class);
        intent.putExtra(WebSocketService.PLAYER_NAME_KEY, state.getPlayerName());
        intent.putExtra(WebSocketService.GAME_ID_KEY, state.getGameID());
        mContext.startService(intent);
        mContext.bindService(intent, this, 0);
    }

    public void connect() {
        if (WebSocketService.getInstance() != null) {
            WebSocketService.getInstance().setParentActivity(mContext);
            handlemissedWin();
        }
    }

    public void disconnect() {
        if (WebSocketService.getInstance() != null)
            WebSocketService.getInstance().setParentActivity(null);
    }

    private void handlemissedWin() {
        WebSocketService service = WebSocketService.getInstance();

        if (service.hasWinner()) {
            String winMessage;
            if (service.isLocalWin())
                winMessage = mContext.getString(R.string.win_message);
            else
                winMessage = service.getLastWinner() + " " + mContext.getString(R.string.lose_message);

            mContext.displayWinMessage(winMessage);
        }

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        WebSocketService.LocalBinder binder = (WebSocketService.LocalBinder)iBinder;
        mService = binder.getService();
        mService.setParentActivity(mContext);

        handlemissedWin();

        mContext.unbindService(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        //nothing to do here
    }
}
