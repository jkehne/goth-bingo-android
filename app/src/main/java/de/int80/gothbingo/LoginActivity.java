package de.int80.gothbingo;

import android.content.Intent;
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
    private static final String LOG = LoginActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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

    private void handleSignIn() {
        EditText playerNameField = (EditText) findViewById(R.id.playerName);
        EditText gameIDField = (EditText) findViewById(R.id.gameID);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(PLAYER_NAME_KEY, playerNameField.getText().toString().trim());
        intent.putExtra(GAME_ID_KEY, gameIDField.getText().toString().trim());
        startActivity(intent);
    }

}