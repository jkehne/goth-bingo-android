package de.int80.gothbingo;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final String STATE_KEY = GameState.class.getName();
    private GameState state;
    private WebSocketServiceConnection backgroundServiceConnection;
    private ProgressDialog progressDialog;
    private NumPlayersActionProvider numPlayersProvider;

    private static MainActivity currentInstance;

    public static MainActivity getCurrentInstance() {
        return currentInstance;
    }

    public GameState getState() {
        return state;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentInstance = this;

        if (savedInstanceState != null)
            state = (GameState)savedInstanceState.getSerializable(STATE_KEY);

        if (state == null) {
            state = new GameState();

            Intent launchIntent = getIntent();
            state.setPlayerName(launchIntent.getStringExtra(LoginActivity.PLAYER_NAME_KEY));
            state.setGameID(launchIntent.getStringExtra(LoginActivity.GAME_ID_KEY));

            if (FieldContentFetcher.isRunning()) {
                showProgressDialog(getString(R.string.fields_downloading_message));
            } else {
                FieldContentFetcher fetcher = new FieldContentFetcher();
                fetcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            if (FieldContentFetcher.isRunning())
                showProgressDialog(getString(R.string.fields_downloading_message));
            else
                setFieldContents(null, true, null);
        }

        Toolbar myToolbar = findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(myToolbar);

        backgroundServiceConnection = new WebSocketServiceConnection(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        backgroundServiceConnection.connect();

        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(2);
        } else {
            Log.e(TAG, "Failed to get notification manager");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        backgroundServiceConnection.disconnect();
    }

    @Override
    protected void onDestroy() {
        if ((progressDialog != null) && (progressDialog.isShowing()))
            progressDialog.dismiss();

        currentInstance = null;

        super.onDestroy();
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
        ViewGroup parent = findViewById(R.id.BingoFieldLayout);
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

        ViewGroup parent = findViewById(R.id.BingoFieldLayout);

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
        new FieldContentFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_KEY, state);
    }

    private void showDownloadFailureDialog(Throwable t) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        StringBuilder messageBuilder = new StringBuilder(getString(R.string.fields_download_failed_message));
        if (t != null) {
            messageBuilder.append(" (");
            messageBuilder.append(t.getLocalizedMessage());
            messageBuilder.append(")");
        }

        builder.setMessage(messageBuilder.toString());
        builder.setTitle(R.string.fields_download_failed_title);
        builder.setPositiveButton(getResources().getString(R.string.ok_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                handleGameExit();
            }
        });

        builder.create().show();
    }

    public void setFieldContents(ArrayList<String> fields, boolean reload, Throwable error) {
        int row, col;
        ViewGroup root, rowHandle;
        BingoFieldView field;

        if (fields != null) {
            state.setAllFields(fields);
        } else
            fields = state.getAllFields();

        if (fields == null) {
            showDownloadFailureDialog(error);
            return;
        }

        if (!reload) {
            Collections.shuffle(fields);
            resetClickedState();
        }

        root = findViewById(R.id.BingoFieldLayout);

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

        MenuItem numPlayersItem = menu.findItem(R.id.NumPlayersField);
        numPlayersProvider = (NumPlayersActionProvider) MenuItemCompat.getActionProvider(numPlayersItem);

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
            case R.id.ShareButton:
                handleShare();
                return true;
            case R.id.GithubButton:
                handleGithub();
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

    private void handleGithub() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(getString(R.string.github_url)));
        startActivity(browserIntent);
    }

    private void handleShare() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_url));

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)));
    }

    private void handleGameExit() {
        backgroundServiceConnection.getService().stop();
        finish();
    }

    private void handleSuggest() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.suggest_button_text);

        final AppCompatEditText input = new AppCompatEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.suggest_dialog_text);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setView(input);

        builder.setPositiveButton(getResources().getString(R.string.ok_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                new SubmitFieldTask().execute(input.getText().toString());
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.cancel_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        final AlertDialog dialog = builder.create();

        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                dialog.dismiss();
                new SubmitFieldTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, input.getText().toString());
                return true;
            }
        });

        dialog.show();
    }

    void showProgressDialog(String message) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(this);

        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                onBackPressed();
            }
        });

        progressDialog.setMessage(message);
        progressDialog.show();
    }

    void dismissProgressDialog() {
        if ((progressDialog != null) && (progressDialog.isShowing()))
            progressDialog.dismiss();
    }

    void setNumPlayers(int numPlayers) {
        if (numPlayersProvider != null)
            numPlayersProvider.setNumPlayers(numPlayers);
    }
}
