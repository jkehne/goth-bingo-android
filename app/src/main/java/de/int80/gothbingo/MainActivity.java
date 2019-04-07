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
import android.view.inputmethod.InputMethodManager;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements IMainActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final MainActivityPresenter presenter;
    private ProgressDialog progressDialog;
    private NumPlayersActionProvider numPlayersProvider;

    private static MainActivity currentInstance;

    public static MainActivity getCurrentInstance() {
        return currentInstance;
    }

    public MainActivity() {
        presenter = new MainActivityPresenter(this, MainActivityModel.getInstance());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(myToolbar);

        String playerName = null, gameID = null;

        if (savedInstanceState != null) {
            presenter.onActivityRestored();
            return;
        }

        Intent launchIntent = getIntent();
        if (launchIntent != null) {
            playerName = launchIntent.getStringExtra(LoginActivity.PLAYER_NAME_KEY);
            gameID = launchIntent.getStringExtra(LoginActivity.GAME_ID_KEY);
        }

        presenter.onActivityCreated(playerName, gameID);
    }

    @Override
    protected void onStart() {
        super.onStart();

        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(2);
        } else {
            Log.e(TAG, "Failed to get notification manager");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onDestroy() {
        if ((progressDialog != null) && (progressDialog.isShowing()))
            progressDialog.dismiss();

        currentInstance = null;

        presenter.onActivityDestroyed();
        super.onDestroy();
    }

    public void onBingoFieldClick(View view) {
        if (view instanceof BingoFieldView) {
            presenter.onFieldClicked((BingoFieldView)view);
        }
    }

    @Override
    public void showWinMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayWinMessage(message);
            }
        });
    }

    @Override
    public void toggleField(BingoFieldView field) {
        field.toggle();
    }

    @Override
    public void toggleField(int row, int col) {
        ViewGroup parent = findViewById(R.id.BingoFieldLayout);
        TableRow rowHandle = (TableRow)parent.getChildAt(row);
        View field = rowHandle.getChildAt(col);

        if (!(field instanceof BingoFieldView))
            return;

        ((BingoFieldView)field).toggle();
    }

    private void displayWinMessage(String message) {
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

    public void hideWinMessage() {
        findViewById(R.id.WinMessageOverlay).setVisibility(View.GONE);
    }

    public void resetClickedState() {
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
                }
            }
        }
    }

    public void onPlayAgainButtonClick(View view) {
        presenter.onPlayAgainButtonClicked();
    }

    public void showDownloadFailureDialog(Throwable t) {
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

    public void setFieldContents(List<String> fields) {
        int row, col;
        ViewGroup root, rowHandle;
        BingoFieldView field;

        root = findViewById(R.id.BingoFieldLayout);

        for (row = 0; row < root.getChildCount(); ++row) {
            rowHandle = (ViewGroup) root.getChildAt(row);

            for (col = 0; col < rowHandle.getChildCount(); ++col) {
                if (!(rowHandle.getChildAt(col) instanceof BingoFieldView))
                    continue;

                field = (BingoFieldView) rowHandle.getChildAt(col);
                field.setText(fields.get(row * 5 + col));
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
        presenter.onGameExit();
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
                new SubmitFieldTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, input.getText().toString());
                closeKeyboard();
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
                closeKeyboard();
                return true;
            }
        });

        dialog.show();
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public void showProgressDialog(String message) {
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

    public void dismissProgressDialog() {
        if ((progressDialog != null) && (progressDialog.isShowing()))
            progressDialog.dismiss();
    }

    public void setNumPlayers(int numPlayers) {
        if (numPlayersProvider != null)
            numPlayersProvider.setNumPlayers(numPlayers);
    }
}
