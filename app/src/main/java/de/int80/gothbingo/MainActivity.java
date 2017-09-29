package de.int80.gothbingo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBingoFieldClick (View view) {
        if (view instanceof BingoFieldView)
            ((BingoFieldView)view).toggle();
    }
}
