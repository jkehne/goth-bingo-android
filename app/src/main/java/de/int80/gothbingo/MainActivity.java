package de.int80.gothbingo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
        } else {
            setFieldContents(state.getAllFields(), true);

            Intent launchIntent = getIntent();
            state.setPlayerName(launchIntent.getStringExtra(LoginActivity.PLAYER_NAME_KEY));
            state.setGameID(launchIntent.getStringExtra(LoginActivity.GAME_ID_KEY));
        }

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
    }

    public void onBingoFieldClick(View view) {
        if (view instanceof BingoFieldView) {
            BingoFieldView bingoField = (BingoFieldView)view;
            bingoField.toggle();
            state.toggleField(bingoField.getFieldX(), bingoField.getFieldY());

            if (state.hasFullRow())
                displayWinMessage(getString(R.string.win_message));
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
        int row, column;
        TableRow rowHandle;

        findViewById(R.id.WinMessageOverlay).setVisibility(View.GONE);

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

        new FieldContentFetcher(this).execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_KEY, state);
    }

    public void setFieldContents(ArrayList<String> fields, boolean reload) {
        int row, col;
        ViewGroup root, rowHandle;
        BingoFieldView field;

        if (fields != null) {
            state.setAllFields(fields);
        } else
            fields = state.getAllFields();

        if (!reload)
            Collections.shuffle(fields);

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
        finish();
    }
}
