package de.int80.gothbingo;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private final String STATE_KEY = GameState.class.getName();

    public GameState getState() {
        return state;
    }

    private GameState state;

    private WebSocketServiceConnection backgroundServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
            state = (GameState)savedInstanceState.getSerializable(STATE_KEY);

        if (state == null) {
            state = new GameState();

            FieldContentFetcher fetcher = new FieldContentFetcher(this);
            fetcher.execute();

            Intent launchIntent = getIntent();
            state.setPlayerName(launchIntent.getStringExtra(LoginActivity.PLAYER_NAME_KEY));
            state.setGameID(launchIntent.getStringExtra(LoginActivity.GAME_ID_KEY));
        } else {
            setFieldContents(state.getAllFields(), true);
        }

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        backgroundServiceConnection = new WebSocketServiceConnection(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        backgroundServiceConnection.connect();

        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
    }

    @Override
    protected void onStop() {
        super.onStop();

        backgroundServiceConnection.disconnect();
    }

    public void onBingoFieldClick(View view) {
        if (view instanceof BingoFieldView) {
            BingoFieldView bingoField = (BingoFieldView)view;
            bingoField.toggle();
            state.toggleField(bingoField.getFieldX(), bingoField.getFieldY());

            if (state.hasFullRow()) {
                displayWinMessage(getString(R.string.win_message));
                backgroundServiceConnection.getService().handleWin();
            }
        }
    }

    public void displayWinMessage(String message) {
        int row, column;
        TableRow rowHandle;

        ((TextView)findViewById(R.id.WinMessage)).setText(message);
        findViewById(R.id.WinMessageOverlay).setVisibility(View.VISIBLE);
        ViewGroup parent = (ViewGroup)findViewById(R.id.BingoFieldLayout);
        for (row = 0; row < parent.getChildCount(); ++row) {
            rowHandle = (TableRow)parent.getChildAt(row);
            for (column = 0; column < rowHandle.getChildCount(); ++column) {
                rowHandle.getChildAt(column).setClickable(false);
            }
        }
    }

    private void hideWinMessage() {
        findViewById(R.id.WinMessageOverlay).setVisibility(View.GONE);
    }

    private void resetClickedState() {
        int row, column;
        TableRow rowHandle;

        ViewGroup parent = (ViewGroup)findViewById(R.id.BingoFieldLayout);

        for (row = 0; row < parent.getChildCount(); ++row) {
            rowHandle = (TableRow)parent.getChildAt(row);
            for (column = 0; column < rowHandle.getChildCount(); ++column) {
                if (!(rowHandle.getChildAt(column) instanceof BingoFieldView))
                    continue;

                BingoFieldView field = (BingoFieldView)rowHandle.getChildAt(column);
                field.setClickable(true);

                if (field.isChecked()) {
                    field.toggle();
                    state.toggleField(field.getFieldX(), field.getFieldY());
                }
            }
        }
    }

    public void onPlayAgainButtonClick(View view) {
        hideWinMessage();
        backgroundServiceConnection.getService().startNewGame();
        new FieldContentFetcher(this).execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_KEY, state);
    }

    private void showDownloadFailureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.fields_download_failed_message);
        builder.setTitle(R.string.fields_download_failed_title);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                handleGameExit();
            }
        });

        builder.create().show();
    }

    public void setFieldContents(ArrayList<String> fields, boolean reload) {
        int row, col;
        ViewGroup root, rowHandle;
        BingoFieldView field;

        if (fields != null) {
            state.setAllFields(fields);
        } else
            fields = state.getAllFields();

        if (fields == null)
            showDownloadFailureDialog();

        if (!reload) {
            Collections.shuffle(fields);
            resetClickedState();
        }

        root = (ViewGroup) findViewById(R.id.BingoFieldLayout);

        for (row = 0; row < root.getChildCount(); ++row) {
            rowHandle = (ViewGroup) root.getChildAt(row);

            for (col = 0; col < rowHandle.getChildCount(); ++col) {
                if (!(rowHandle.getChildAt(col) instanceof BingoFieldView))
                    continue;

                field = (BingoFieldView) rowHandle.getChildAt(col);
                field.setText(fields.get(row * 5 + col));
                if (state.isFieldChecked(col, row))
                    field.toggle();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.RefreshButton:
                onPlayAgainButtonClick(null);
                return true;
            case R.id.ExitButton:
                handleGameExit();
                return true;
            case R.id.SuggestButton:
                handleSuggest();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        handleGameExit();
    }

    private void handleGameExit() {
        backgroundServiceConnection.getService().stop();
        unbindService(backgroundServiceConnection);
        finish();
    }

    private void handleSuggest() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.suggest_button_text);
        //builder.setMessage(R.string.suggest_dialog_text);

        final AppCompatEditText input = new AppCompatEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.suggest_dialog_text);
        builder.setView(input);

        builder.setPositiveButton(getResources().getString(R.string.ok_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                new SubmitFieldTask(MainActivity.this).execute(input.getText().toString());
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.cancel_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }
}
