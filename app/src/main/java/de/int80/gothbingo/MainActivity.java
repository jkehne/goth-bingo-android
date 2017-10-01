package de.int80.gothbingo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;

public class MainActivity extends AppCompatActivity {
    private final String STATE_KEY = GameState.class.getName();

    private GameState state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
            state = (GameState)savedInstanceState.getSerializable(STATE_KEY);

        if (state == null)
            state = new GameState();
    }

    public void onBingoFieldClick(View view) {
        int row, column;
        TableRow rowHandle;

        if (view instanceof BingoFieldView) {
            BingoFieldView bingoField = (BingoFieldView)view;
            bingoField.toggle();
            state.toggleField(bingoField.getFieldX(), bingoField.getFieldY());

            if (state.hasFullRow()) {
                findViewById(R.id.WinMessageOverlay).setVisibility(View.VISIBLE);
                ViewGroup parent = (ViewGroup)findViewById(R.id.BingoFieldLayout);
                for (row = 0; row < parent.getChildCount(); ++row) {
                    rowHandle = (TableRow)parent.getChildAt(row);
                    for (column = 0; column < rowHandle.getChildCount(); ++column) {
                        rowHandle.getChildAt(column).setClickable(false);
                    }
                }
            }
        }
    }

    public void onPlayAgainButtonClick(View view) {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_KEY, state);
    }
}
