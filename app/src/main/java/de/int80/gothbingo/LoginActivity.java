package de.int80.gothbingo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    static final String PLAYER_NAME_KEY = LoginActivity.class.getName() + ".PLAYER_NAME";
    static final String GAME_ID_KEY = LoginActivity.class.getName() + ".GAME_ID";

    private void setEventListeners() {
        Button signInButton = (Button)findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignIn();
            }
        });

        EditText gameIDField = (EditText)findViewById(R.id.gameID);
        gameIDField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                handleSignIn();
                return true;
            }
        });
    }

    private void restoreLoginInfo() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        EditText playerNameField = (EditText)findViewById(R.id.playerName);
        playerNameField.setText(prefs.getString(PLAYER_NAME_KEY, ""));

        EditText gameIDField = (EditText)findViewById(R.id.gameID);
        gameIDField.setText(prefs.getString(GAME_ID_KEY, ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setEventListeners();

        restoreLoginInfo();
    }

    private String extractFieldContents(int fieldID) {
        EditText field = (EditText) findViewById(fieldID);

        if (field == null)
            return null;

        return field.getText().toString().trim();
    }

    private void handleSignIn() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(PLAYER_NAME_KEY, extractFieldContents(R.id.playerName));
        intent.putExtra(GAME_ID_KEY, extractFieldContents(R.id.gameID));
        startActivity(intent);
    }

    private void saveLoginInfo(String playerName, String gameID) {
        SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();

        if (playerName != null)
            prefs.putString(PLAYER_NAME_KEY, playerName);

        if (gameID != null)
            prefs.putString(GAME_ID_KEY, gameID);

        prefs.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();

        saveLoginInfo(extractFieldContents(R.id.playerName), extractFieldContents(R.id.gameID));
    }
}