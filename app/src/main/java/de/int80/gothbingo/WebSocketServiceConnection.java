package de.int80.gothbingo;

import android.content.ComponentName;
import android.content.Context;
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
    private final IMainActivityModel mainActivityModel;

    WebSocketServiceConnection(IMainActivityModel model) {
        mainActivityModel = model;

        if (WebSocketService.getInstance() != null) {
            return;
        }

        Context appContext = GothBingo.getContext();

        Intent intent = new Intent(appContext, WebSocketService.class);
        intent.putExtra(WebSocketService.PLAYER_NAME_KEY, model.getPlayerName());
        intent.putExtra(WebSocketService.GAME_ID_KEY, model.getGameID());
        appContext.startService(intent);
        appContext.bindService(intent, this, 0);
    }

    private void handlemissedWin() {
        WebSocketService service = WebSocketService.getInstance();

        if (service.hasWinner()) {
            String winMessage;
            if (service.isLocalWin())
                winMessage = GothBingo.getContext().getString(R.string.win_message);
            else
                winMessage = service.getLastWinner() + " " + GothBingo.getContext().getString(R.string.lose_message);

            mainActivityModel.showWinMessage(winMessage);
        }

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        WebSocketService.LocalBinder binder = (WebSocketService.LocalBinder)iBinder;
        mService = binder.getService();
        mService.setActivityModel(mainActivityModel);

        handlemissedWin();

        GothBingo.getContext().unbindService(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        //nothing to do here
    }
}
