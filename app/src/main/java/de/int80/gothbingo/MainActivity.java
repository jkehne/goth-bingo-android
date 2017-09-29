package de.int80.gothbingo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private final String STATE_KEY = GameState.class.getName();

    private GameState state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBingoFieldClick (View view) {
        if (view instanceof BingoFieldView) {
            BingoFieldView bingoField = (BingoFieldView)view;
            bingoField.toggle();
            state.toggleField(bingoField.getFieldX(), bingoField.getFieldY());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_KEY, state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        state = (GameState)savedInstanceState.getSerializable(STATE_KEY);
    }
}
