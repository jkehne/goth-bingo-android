package de.int80.gothbingo;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Pair;

import java.io.IOException;
import java.util.List;

class MainActivityPresenter implements IMainActivityPresenter {

    private IMainActivity view;
    private IMainActivityModel model;

    MainActivityPresenter(IMainActivity view, IMainActivityModel model)
    {
        this.view = view;
        this.model = model;
        model.setPresenter(this);
    }

    public void onActivityCreated(String playerName, String gameID) {
        model.onActivityCreated(playerName, gameID);
    }

    @Override
    public void onActivityRestored() {
        this.resetBoard(model.getState());
    }

    void onActivityDestroyed()
    {
        model.setPresenter(new BackgroundActivityPresenter());
    }

    public void showWinMessage(String message) {
        view.showWinMessage(message);
        playWinSound();
    }

    @Override
    public void onPlayAgainButtonClicked() {
        view.hideWinMessage();
        model.startNewGame();
    }

    @Override
    public void resetBoard(GameState state) {
        view.resetClickedState();
        view.setFieldContents(state.getAllFields());
        this.setCheckedFields(state.getCheckedFieldsList());
    }

    private void setCheckedFields(List<Pair<Integer, Integer>> checkedFields) {
        for (Pair<Integer, Integer> field : checkedFields) {
            view.toggleField(field.first, field.second);
        }
    }

    @Override
    public void onFieldsDownloadFailed(Throwable error) {
        view.showDownloadFailureDialog(error);
    }

    @Override
    public void showProgressDialog(String message) {
        view.showProgressDialog(message);
    }

    @Override
    public void dismissProgressDialog() {
        view.dismissProgressDialog();
    }

    @Override
    public void onGameExit() {
        model.onGameExit();
    }

    @Override
    public void onNumPlayersChanged(int numPlayers) {
        this.view.setNumPlayers(numPlayers);
    }

    void onFieldClicked(BingoFieldView field) {
        view.toggleField(field);
        model.handleFieldClicked(field.getFieldX(), field.getFieldY());
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
}
